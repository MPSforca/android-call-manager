# Architecture

Call Manager is a single-module Android app (Kotlin, Jetpack Compose, Hilt, Room) that registers as
the device's `CallScreeningService`. It adds rule-based call blocking and keeps every blocked call
visible in-app.

## Components

All paths are under `app/src/main/java/com/sforca/callmanager/`.

| Component | Role |
|---|---|
| `screening/RuleBasedCallScreeningService` | The blocking logic. Telecom binds it only when a real call arrives; it is not a running or foreground service. `onScreenCall` calls `CallRepository.evaluate()`, maps the result to a `ScreeningAction` via `ScreeningPolicy`, and replies with a `CallResponse`. |
| `screening/PhoneStateReceiver` | Manifest broadcast receiver (not a service) watching RINGING → OFFHOOK to detect the user answering a call, feeding the "recently accepted" rule. |
| `core/data/CallRepository` | Single source of truth for rule evaluation. Dependencies (including the `ContactsLookup` and `RecentOutgoingLookup` interfaces) are constructor-injected, so it is unit-testable without a device. |
| `core/data/ScreeningPolicy` | Pure function mapping a `ScreeningDecision` (Allow/Reject) plus the user's `BlockedCallAction` setting to a `ScreeningAction` (ALLOW/SILENCE/REJECT). No Android types. |
| `core/data/BlockedCallMerger` | Pure function merging the app's own Room block log with calls the *system* call log tagged `BLOCKED_TYPE`, deduplicating calls the app blocked itself. |
| `core/telephony/` | Android-backed lookups: contacts, call log, outgoing calls, phone-number formatting, battery optimization, rejected-call notification. |
| `core/database/` | Room database, one file per entity and DAO, migrations. |
| `core/datastore/` | User settings (`SettingsStore`). |
| `core/di/` | Hilt modules. |
| `feature/{recent,lists,rules,debug,onboarding}/` | One screen plus ViewModel per feature. Tabs: Recent (batch-block from call history), Lists (block/allow lists, contact picker), Rules (settings), Test a number (`feature/debug`, all builds; simulate a call against the rules without dialing). |

## Rule evaluation order

`CallRepository.evaluate()` returns on the first match:

1. block list → reject
2. allow list → allow
3. caller-name pattern (carrier-supplied caller ID name) → allow
4. known contact, if enabled → allow
5. answered within N days, if enabled → allow
6. outgoing call within N days, if enabled → allow
7. fallback: reject if "block unknown numbers" is on, else allow

A "reject" decision is then turned into silence or reject by `ScreeningPolicy`.

## Design decisions

Each of these is deliberate. Read the linked record before proposing to change one; if you still
want to, write a new ADR (see [adr/README.md](adr/README.md)).

- **No foreground or persistent service.** [ADR 0001](adr/0001-no-foreground-service.md)
- **Only real SIM/carrier calls are screened, never VoIP.** [ADR 0002](adr/0002-no-voip-screening.md)
- **Blocked calls merge two sources; the app does not take the default-dialer role.**
  [ADR 0003](adr/0003-blocked-calls-merge-sources.md)
- **Silence vs reject, and why the incoming-call UI is out of our hands.**
  [ADR 0004](adr/0004-silence-vs-reject.md)

### Smaller decisions

**Recent calls and call history are device-wide.** `CallLogUtil.recentCalls()` reads
`CallLog.Calls.CONTENT_URI`, Android's persistent system call log, not app-runtime state. It shows
the same history whether or not the app was running when a call happened.

**"Recently called" reads the system call log; "recently accepted" uses our own table.** Android has
no queryable "answered" record, so `PhoneStateReceiver` writes accepted callers to Room
(`accepted_callers`). Outgoing calls are already in the system call log (`OUTGOING_TYPE`), so
`AndroidRecentOutgoingLookup` queries that at screening time: no receiver, table or migration, and
it covers calls made before install. Numbers are matched with `PhoneNumberUtils.compare`, since the
call log stores them as dialed while incoming ones arrive as E.164.

**System integrations sit behind interfaces.** `ContactsLookup` and `RecentOutgoingLookup` exist so
the rule logic can be unit tested with fakes instead of a real `ContentResolver`.

## Known limitations

- Android's native block list can't be *listed* by this app (that needs the default-dialer role).
  Only individual blocked calls surfaced through the call log are shown, plus single-number checks
  via `BlockedNumberContract.isBlocked()` if ever needed.
- Whether a silenced call appears full-screen or as a notification is decided by the device's
  dialer, not this app.
- `BLOCK_REASON` exists only on API 29+; older versions show a generic "Blocked by Android" label.
