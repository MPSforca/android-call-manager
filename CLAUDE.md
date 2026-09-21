# Call Manager

Android app (Kotlin + Jetpack Compose) that acts as the device's `CallScreeningService` to add
rule-based call blocking beyond what stock Android offers, while keeping every blocked call visible
in-app. First Android project for the user (matheussforza@gmail.com) — assume no prior Android
platform knowledge when explaining things, though general software engineering background is solid.

## Architecture

- `service/RuleBasedCallScreeningService` — the actual blocking logic. Telecom binds this only when
  a real call arrives; it is not a running/foreground service. `onScreenCall` evaluates rules via
  `CallRepository.evaluate()`, maps the result to a `ScreeningAction` via `ScreeningPolicy`, and
  replies with a `CallResponse`.
- `service/PhoneStateReceiver` — manifest broadcast receiver (not a service) that watches for
  RINGING → OFFHOOK to detect the user answering a call, feeding the "recently accepted" rule.
- `data/CallRepository` — single source of truth for rule evaluation. Takes `ContactsLookup` and
  `CallManagerDatabase` as constructor params (defaulted to real Android-backed implementations) so
  it's unit-testable without a device.
- `data/ScreeningPolicy` — pure function mapping `ScreeningDecision` (Allow/Reject) + the user's
  `BlockedCallAction` setting to a `ScreeningAction` (ALLOW/SILENCE/REJECT). No Android types, so no
  Robolectric needed to test it.
- `data/BlockedCallMerger` — pure function merging our own Room block log with calls the *system*
  call log tagged `BLOCKED_TYPE` (see below), deduping calls we blocked ourselves.
- `ui/MainViewModel` + `ui/CallManagerApp` — four tabs: Recent (batch-block from call history),
  Lists (block/allow lists, contact picker), Rules (settings), Debug (debug builds only — simulate
  a call against the rules without dialing anything).

## Key design decisions (don't relitigate these without reading why)

**No foreground/persistent service, by design.** The user asked for reliable background operation
and battery efficiency. Those point the same direction: `CallScreeningService` and the phone-state
`BroadcastReceiver` are both woken on-demand by the system; there is nothing to poll and no idle
process. A foreground service would *hurt* battery for no benefit here. The one real risk is OEM
battery managers (Xiaomi/Samsung/Huawei/etc.) killing the app hard enough that Telecom can't even
cold-start it for a call — mitigated by prompting the user (Rules tab) to exempt the app via
`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, which is not a wakelock or foreground service.

**Only screens real SIM/carrier calls, never VoIP apps (WhatsApp etc.).** The manifest does not
declare `android.telecom.extra.CALL_SCREENING_INCLUDE_SELF_MANAGED_CALLS`, so Telecom never routes
self-managed connections (how VoIP apps integrate) to `onScreenCall` at all. There's also a defensive
`PROPERTY_SELF_MANAGED` check in the service itself as a second layer. Do not add that manifest
opt-in without an explicit user request — it would start screening VoIP calls too.

**"Blocked calls" merges two sources, not one.** Third-party apps cannot enumerate Android's native
block list (`BlockedNumberContract`) — reading the full list requires the default-dialer role, which
this app deliberately does not request (too large a scope/risk for what was asked). Instead, every
blocked call — from any source (this app, Android's own block list, the carrier, another
screening app) — gets tagged `CallLog.Calls.TYPE = BLOCKED_TYPE` in the *system* call log, which any
app holding `READ_CALL_LOG` can read. `BlockedCallMerger` combines that with our own Room log (which
has richer per-rule reasons) and dedupes by number + 5-second timestamp window. `BLOCK_REASON` is
only available API 29+; older versions get a generic "Blocked by Android" label.

**Recent calls / call history is already device-wide.** `CallLogUtil.recentCalls()` reads
`CallLog.Calls.CONTENT_URI`, Android's persistent system call log — not app-runtime state. It shows
the same history regardless of whether the app was running when a call happened.

**SILENCE vs REJECT for blocked calls.** `CallResponse.setSilenceCall(true)` (API 29+) lets a call
stay visible on-screen/in-notifications but suppresses ringtone/vibration — this is the default
behavior for anything that fails the allow rules. Below API 29, silencing isn't available, so it
falls back to a full reject. The user can switch to REJECT everywhere via a Rules-tab setting.

**Full-screen vs notification-only rendering of a silenced call is out of this app's control.**
`CallScreeningService` (what this app implements) only gets to pick ALLOW/SILENCE/REJECT — it does
not draw the incoming-call UI. Whether a live (non-rejected) call shows full-screen or as a plain
notification is decided entirely by whichever app holds the **default Dialer / InCallService**
role (stock Phone app or the OEM dialer), based on device state (locked vs. in-use) and its own UI
logic, not by anything a `CallScreeningService` sets on `CallResponse`. Getting "silenced but
notification-only, still answerable" would require this app to become the default dialer and render
its own InCallUI — a deliberately unrequested scope, see the block-list-visibility decision above.
Tried and reverted: rejecting silenced calls outright (`setDisallowCall`/`setRejectCall` +
`setSkipNotification(false)`) does avoid the full-screen UI, but it also stops the call from
ringing/being answerable at all, which is not what "silence" is supposed to mean here.

**"Recently called" reads the system call log; "recently accepted" uses our own table.** Accepted
callers are recorded by `PhoneStateReceiver` into Room (`accepted_callers`) because Android has no
"answered" record we can query. Outgoing calls, by contrast, are already in the system call log
(`OUTGOING_TYPE`), so `AndroidRecentOutgoingLookup` queries that at screening time — no receiver, no
table, no migration, and it covers calls made before install. Behind the `RecentOutgoingLookup`
interface for unit testing. Numbers are matched with `PhoneNumberUtils.compare` since the call log
stores them as dialed while incoming ones arrive as E.164.

**Contacts access is behind an interface.** `ContactsLookup`/`AndroidContactsLookup` exist purely so
`CallRepository`'s rule logic can be unit tested with a fake instead of a real `ContentResolver`.

## Environment (this machine)

- Android Studio + SDK installed via `brew install --cask android-studio`; SDK at
  `~/Library/Android/sdk`, env vars in `~/.zshrc`.
- Gradle wrapper uses Gradle 8.10.2 (required by AGP 8.7.3, which needs Gradle ≥ 8.9).
- **Gradle/Kotlin compilation needs JDK 17–21, not the system default.** The system `java` and even
  Android Studio's bundled JBR resolved to JDK 25 on this machine, which the Gradle version in use
  doesn't support. Build with `JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.2-tem` (installed via
  SDKMAN) — see README for the exact commands.
- Kotlin 2.0 requires the separate `org.jetbrains.kotlin.plugin.compose` Gradle plugin; Compose no
  longer works via `composeOptions.kotlinCompilerExtensionVersion` alone.
- `buildFeatures.buildConfig = true` is required explicitly (AGP 8.x opt-in) — needed here because
  the Debug tab is gated on `BuildConfig.DEBUG`.

## Testing

- Unit tests run on the JVM, no emulator: `./gradlew testDebugUnitTest`.
- **Robolectric is pinned to sdk = [34] via `@Config`** on `CallRepositoryTest` — Robolectric 4.13
  doesn't yet support API 35, which is this project's `compileSdk`/`targetSdk`. Bump this only when
  upgrading the Robolectric dependency to a version that supports 35.
- Pure logic (`ScreeningPolicy`, `BlockedCallMerger`) is tested with plain JUnit — no Robolectric —
  by design; keep new rule/merge logic Android-free where possible so it stays cheap to test.
- The in-app Debug tab (debug builds only) runs the real `CallRepository.evaluate()` against a typed
  number — useful for interactive testing beyond the automated suite. See README for adb-based
  fixtures (seeding contacts, simulating incoming calls).
- `CallLogUtil`'s `recentCalls`/`systemBlockedCalls`/`outgoingNumbersSince` (raw `CallLog.Calls.CONTENT_URI` queries,
  including their LIMIT/OFFSET paging) are **not** Robolectric-testable — confirmed experimentally:
  Robolectric's `CallLog` shadow doesn't back a real queryable table, so even a plain insert-then-
  query returns 0 rows. Only the pure post-processing helpers (`groupByNumber`, `callTypeLabel`) are
  unit tested; the query methods are manual/adb-tested like `AndroidContactsLookup`, see README.

## Known limitations / things not implemented

- Android's native block list itself still can't be *listed* by this app (needs default-dialer role,
  not requested) — only individual blocked calls surfaced via the call log, and single-number checks
  via `BlockedNumberContract.isBlocked()` if ever needed.
