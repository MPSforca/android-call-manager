# Testing

## Automated unit tests (fast, no device needed)

```bash
./gradlew testDebugUnitTest
```

These cover rule evaluation (`CallRepositoryTest`), the silence-vs-reject decision
(`ScreeningPolicyTest`), merging the app's blocked-call log with Android's system call log
(`BlockedCallMergerTest`), and the ViewModels. They run on the JVM via Robolectric; no emulator,
contacts or real calls are required. Report: `app/build/reports/tests/testDebugUnitTest/index.html`.

Conventions:

- Pure logic (`ScreeningPolicy`, `BlockedCallMerger`) is tested with plain JUnit, no Robolectric,
  by design. Keep new rule and merge logic Android-free where possible.
- `CallRepositoryTest` is pinned to `@Config(sdk = [34])` because Robolectric 4.13 doesn't support
  API 35. Bump the pin only when upgrading Robolectric.
- `CallLogUtil`'s `recentCalls` / `systemBlockedCalls` / `outgoingNumbersSince` (raw
  `CallLog.Calls.CONTENT_URI` queries, including LIMIT/OFFSET paging) are **not**
  Robolectric-testable: its `CallLog` shadow doesn't back a real queryable table, so even a plain
  insert-then-query returns 0 rows. Only the pure helpers (`groupByNumber`, `callTypeLabel`) are unit
  tested; the query methods are tested manually with the adb recipes below, as is
  `AndroidContactsLookup`.

## Manual and interactive testing

Real-device testing needs an incoming call, a contact list and existing call history, all awkward
to set up by hand. Here is how to fake each. Recipes that use `run-as` need a debug build.

**1. Simulate the rule engine directly (fastest loop)**

The **Test** tab (bottom nav, also reachable from the Rules hub's "Test a number" button or the
`callmanager://test` deep link) is available in every build. Type any phone number and tap "Test number": it runs the exact same
`CallRepository.evaluate()` logic the real `CallScreeningService` uses, including writing a rejected
entry into the Blocked tab if the rules say to block it. Use this to iterate on rules without
touching the phone/call stack at all.

**2. Simulate an actual incoming call (end-to-end, needs an emulator or a second phone)**

- Emulator: click `...` (Extended Controls) → **Phone** → enter a number → "Call Device". This rings
  the emulator for real and goes through Telecom → your `CallScreeningService`, exactly like a real
  call.
- Or from a terminal, with the emulator running:
  ```bash
  adb emu gsm call +15551234567     # ring it
  adb emu gsm cancel +15551234567   # hang up
  ```
- On a real second phone: just call the test device from it.

**3. Seed fake contacts on the emulator**

- Easiest: open the emulator's Contacts app and add a few by hand.
- Scripted, for one name + number — insert a raw contact, then attach a name row and a phone row to
  it via `ContactsContract.Data` (this is what the Contacts app itself does under the hood):
  ```bash
  # 1. Create an empty raw contact and note the _id it prints (e.g. "Row: 0 _id=12").
  adb shell content insert --uri content://com.android.contacts/raw_contacts \
    --bind account_type:s:null --bind account_name:s:null

  RAW_ID=12   # substitute the _id from step 1

  # 2. Attach a display name.
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:$RAW_ID \
    --bind mimetype:s:vnd.android.cursor.item/name \
    --bind data1:s:"Test Contact"

  # 3. Attach a phone number (data2:i:2 = TYPE_HOME; use 2 for mobile-style testing).
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:$RAW_ID \
    --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
    --bind data1:s:+15551234567 \
    --bind data2:i:2
  ```
  Use this to test "known contact" behavior end-to-end: the Test tab's `allowContacts` rule, and
  the block/allow list falling back to this name when you add the same number manually (see
  `CallRepository.blockNumbers`/`allowNumbers`).
- If you need to seed many contacts repeatably, it's more reliable to generate a `.vcf` file and
  import it:
  ```bash
  cat > /tmp/test_contacts.vcf <<'EOF'
  BEGIN:VCARD
  VERSION:3.0
  FN:Test Contact
  TEL;TYPE=CELL:+15551234567
  END:VCARD
  EOF
  adb push /tmp/test_contacts.vcf /sdcard/Download/test_contacts.vcf
  # then on-device: Contacts app → Fix & manage → Import → From .vcf file
  ```

**4. Seed the allow list directly (test allowlisting numbers that aren't contacts)**

The Allow tab already lets you type any number by hand or pick one from Contacts, but for
repeatable testing it's faster to write straight into the app's Room database
(`allow_list` table) via `run-as` + `sqlite3` — this only works on a debug build, since
`run-as` requires the app to be debuggable:

```bash
./scripts/seed_allow_list.sh                              # seeds a default fixture set
./scripts/seed_allow_list.sh +15551234567 "Some Name"      # or: seed one specific entry
```

This is the scenario the allow list exists for: a number with **no** matching contact should
still ring through as normal, because it's on the allow list — not because of the "allow known
contacts" rule. To verify:

- Open the Lists tab and confirm the seeded numbers show up under "Allow list".
- Test tab → type one of the seeded numbers → "Test number" → should report
  `ALLOWED — On your allow list`.
- Open the Recent tab (seed some call history first, see below, from the same numbers) and
  confirm those rows are highlighted as allowed, with a checkmark icon.

To inspect or clear it manually:

```bash
adb shell run-as com.matheus.callmanager sqlite3 databases/call-manager.db \
  "SELECT * FROM allow_list;"
adb shell run-as com.matheus.callmanager sqlite3 databases/call-manager.db \
  "DELETE FROM allow_list;"
```

**5. Seed fake call history**

The Recent tab and the Blocked tab both read Android's real system call log
(`CallLog.Calls.CONTENT_URI`) — not app state — so history persists across app reinstalls and shows
up regardless of whether the app was running when a call happened. To add fake entries for testing:

```bash
adb shell content insert --uri content://call_log/calls \
  --bind number:s:+15551234567 \
  --bind date:l:$(($(date +%s%N)/1000000)) \
  --bind duration:i:0 \
  --bind type:i:6 \
  --bind new:i:1
```

`type:i:6` is `CallLog.Calls.BLOCKED_TYPE` — use this to fake a call that Android itself (or some
other app) blocked, to test that it shows up correctly in the Blocked tab as source "Android" (as
opposed to "Call Manager", which is only used for calls this app's own rules rejected). Use `type:i:1`
for a normal incoming call, `type:i:2` for outgoing, `type:i:3` for missed.

**6. Seed a batch of recent calls at once (for the grouped Recent tab)**

The Recent tab groups calls by number, so testing it needs repeat calls from the same number plus
several distinct numbers. `scripts/seed_call_log.sh` inserts a batch in one shot: a repeat caller
(3 calls, to exercise the "N calls" grouping/expansion) plus a few one-off numbers. Add one of the
seeded numbers to your block list afterwards (Lists tab, or the Test tab) to check the blocked-row
styling too:

```bash
./scripts/seed_call_log.sh                  # seeds the default fixture set
./scripts/seed_call_log.sh +15559876543 5   # or: seed N calls from one specific number
```

To clear it back out: `adb shell content delete --uri content://call_log/calls`. This wipes the
*entire* device call log, real entries included — only use it on an emulator or a throwaway test
device.

**7. Testing pagination in the Recent/Blocked tabs**

Both tabs page through the system call log in batches of `CallLogUtil.DEFAULT_PAGE_SIZE` (200), with
a "Load more" button that appears when a full page was returned. To exercise it without waiting for
real call history to accumulate, seed more than one page's worth in a loop:

```bash
for i in $(seq 1 220); do
  ./scripts/seed_call_log.sh +1555000$(printf '%04d' "$i") 1
done
```

Open the Recent tab: it should show the first 200 grouped rows plus a "Load more" button; tapping it
should fetch the rest with no duplicates or gaps. Mark some of the seeded numbers `type:i:6`
(`BLOCKED_TYPE`, see above) to test the same "Load more" behavior on the Blocked tab.

**8. Re-run onboarding (first-run flow and the missing-permissions banner)**

Onboarding only appears when the screening role or a runtime permission is missing, so an
already-set-up emulator opens straight on the Recent tab. `pm clear` wipes app data and revokes
runtime permissions but **leaves the screening role**, so the flow would start at step 2. Remove
the role as well to start from step 1:

```bash
# Check who holds the call-screening role (prints the package name, or nothing)
adb shell cmd role get-role-holders android.app.role.CALL_SCREENING

adb shell cmd role remove-role-holder android.app.role.CALL_SCREENING com.matheus.callmanager
adb shell pm clear com.matheus.callmanager
adb shell am start -n com.matheus.callmanager/.MainActivity
```

You should see "Step 1 of 3" (screening role), then permissions, then the battery step. The
battery step is optional and first-run only, and is skipped on API < 29, where the role doesn't
exist. To give the role back without going through the flow:

```bash
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING com.matheus.callmanager
```

To test the `MissingPermissionsBanner` instead, choose "Skip for now" on the role and permissions
steps: the app opens on the tabs with the banner on top, and its "Fix" button re-requests the
missing item. If the app doesn't look new at all, make sure the latest build was actually installed
(`./gradlew installDebug`, or Run in Android Studio); `assembleDebug` alone doesn't update the
emulator.

**9. Deep links and navigation**

The app handles `callmanager://recent`, `callmanager://lists`, `callmanager://rules` and
`callmanager://call/{number}` (URL-encode the number, so `+` becomes `%2B`):

```bash
adb shell am start -a android.intent.action.VIEW -d "callmanager://rules"
adb shell am start -a android.intent.action.VIEW -d "callmanager://call/%2B15551234567"
```

Call detail should open as its own screen with no bottom bar, and system back should return to the
previous screen. Also check that:

- the navigation bar becomes a rail when the emulator is rotated to landscape, or on a tablet AVD;
- switching tabs keeps each tab's scroll position and back stack;
- rotating while on call detail keeps you on that call.
