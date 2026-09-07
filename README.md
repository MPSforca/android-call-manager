# Call Manager

An Android call-screening app: block calls by rule (explicit block list, explicit allow list, known
contacts, "answered recently" window) while keeping every blocked call visible in-app, merged with
whatever Android's own call log already knows about.

## Requirements

- Android Studio (bundles the SDK) — installed at `/Applications/Android Studio.app` on this machine.
- A JDK in the 17–21 range for Gradle itself. The system default and Android Studio's bundled JBR may
  resolve to a newer JDK that this project's Gradle version doesn't support — see "Troubleshooting"
  below if `./gradlew` fails immediately.

## Running it

### From Android Studio (recommended for day-to-day development)

1. Open the project folder in Android Studio.
2. Create a virtual device (Device Manager → Create Device) or connect a real phone with USB
   debugging enabled.
3. Click Run.
4. On first launch, grant the requested permissions, then tap through the prompt to set the app as
   your device's call-screening app (required — otherwise no call is ever intercepted).

### From the command line

```bash
export JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.2-tem   # or any JDK 17–21
export ANDROID_HOME=$HOME/Library/Android/sdk

./gradlew assembleDebug            # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug             # builds and installs onto a connected device/emulator
```

## Testing

### Automated unit tests (fast, no device needed)

```bash
export JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.2-tem
export ANDROID_HOME=$HOME/Library/Android/sdk

./gradlew testDebugUnitTest
```

This covers the rule-evaluation logic (`CallRepositoryTest`), the silence-vs-reject decision
(`ScreeningPolicyTest`), and merging the app's own blocked-call log with Android's system call log
(`BlockedCallMergerTest`). These run on the JVM via Robolectric — no emulator, no real contacts, no
real calls required. Test report: `app/build/reports/tests/testDebugUnitTest/index.html`.

### Manual/interactive testing with fixtures and mocks

Real device testing needs three things that are awkward to set up by hand: an incoming call, a
contact list, and existing call history. Here's how to fake each one.

**1. Simulate the rule engine directly (fastest loop)**

Debug builds have a **Debug** tab (bottom nav, only visible when `BuildConfig.DEBUG` is true — i.e.
never in a release build). Type any phone number and tap "Simulate call": it runs the exact same
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
  Use this to test "known contact" behavior end-to-end: the Debug tab's `allowContacts` rule, and
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
- Debug tab → type one of the seeded numbers → "Simulate call" → should report
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
seeded numbers to your block list afterwards (Lists tab, or the Debug tab) to check the blocked-row
styling too:

```bash
./scripts/seed_call_log.sh                  # seeds the default fixture set
./scripts/seed_call_log.sh +15559876543 5   # or: seed N calls from one specific number
```

To clear it back out: `adb shell content delete --uri content://call_log/calls`. This wipes the
*entire* device call log, real entries included — only use it on an emulator or a throwaway test
device.

## Troubleshooting

- **`./gradlew` fails immediately with a Java version error** — your default `java` is likely newer
  than this Gradle version supports. Point `JAVA_HOME` at a JDK 17–21 explicitly (see commands
  above). `sdk list java` (via SDKMAN, if installed) or Android Studio's own JBR (check its actual
  version first — it is not guaranteed to be in range on every install) can provide one.
- **Robolectric tests fail with an SDK version error** — `CallRepositoryTest` is pinned to
  `@Config(sdk = [34])` because Robolectric doesn't yet support this project's `compileSdk` (35).
  This is expected; don't remove the pin without upgrading Robolectric first.
- **Calls aren't being screened at all on a real device** — go to the Rules tab and check the
  battery-optimization banner; some OEMs (Xiaomi, Samsung, Huawei, etc.) kill the app process
  aggressively enough that Telecom can't wake it for a call. Exempting the app from battery
  optimization there fixes it without needing a foreground service.

See `CLAUDE.md` for architecture and the reasoning behind the bigger design decisions.
