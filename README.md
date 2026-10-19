# Call Manager

[![CI](https://github.com/MPSforca/android-call-manager/actions/workflows/pr.yml/badge.svg?branch=main&event=push)](https://github.com/MPSforca/android-call-manager/actions/workflows/pr.yml?query=branch%3Amain)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

Rule-based call blocking for Android, built on the system's call-screening role. Stop spam calls
without losing track of them: every blocked call stays visible in the app, with the reason it was
blocked.

**Who it's for:** people who get a lot of unwanted calls but don't want to miss real ones, and
don't want an always-running app (or an account, or a server) to deal with it.

<!-- TODO: screenshots / GIF of the Recent, Lists and Rules tabs -->

## Features

- **Block list and allow list**, managed by hand, from a contact picker, or in bulk from your call
  history.
- **Smart allow rules:** known contacts, caller-name patterns (e.g. "Dr.*"), people you answered
  recently, people you called recently.
- **Optional "block everyone else"** mode for strict screening.
- **Silence or reject:** by default blocked calls ring silently and stay answerable; switch to
  outright rejection in the Rules tab.
- **Nothing disappears:** blocked calls are listed in-app, merged with calls that Android itself,
  your carrier, or other apps blocked.
- **Zero background cost:** no foreground service, no polling. Android wakes the app only when a
  call arrives.
- **Phone calls only:** WhatsApp and other VoIP calls are never screened.
- **Private:** everything stays on your device. See [PRIVACY.md](PRIVACY.md).

## How a call is decided

When a call comes in, the rules run in this order and the **first match wins**:

1. **On your block list** → blocked.
2. **On your allow list** → allowed.
3. **Caller name matches an allow pattern** → allowed.
4. **Known contact** (if "allow contacts" is on) → allowed.
5. **Answered within the last N days** (if enabled) → allowed.
6. **You called them within the last N days** (if enabled) → allowed.
7. **Otherwise:** blocked if "block unknown numbers" is on, allowed if not.

"Blocked" then becomes *silenced* or *rejected* depending on your setting. Because the block list is
checked first, a number on both lists is blocked.

## Install

There is no store listing yet. Either:

- Download the latest APK from the [Releases](https://github.com/MPSforca/android-call-manager/releases) page and install it, or
- [Build it yourself](docs/DEVELOPMENT.md).

On first launch, grant the permissions below and accept the prompt to make Call Manager your
device's **call-screening app**. Without that role Android never hands it a call.

On phones from Xiaomi, Samsung, Huawei and some other makers, also exempt the app from battery
optimization (the Rules tab prompts for it). Aggressive battery managers can otherwise kill the app
so hard that Android can't start it for an incoming call.

Requires Android 8.0 (API 26) or newer. Silencing (as opposed to rejecting) needs Android 10+.

## Permissions and why

| Permission | Why it's needed |
|---|---|
| `READ_CALL_LOG` | Shows your call history, lists calls blocked by Android or your carrier, and powers the "you called them recently" rule. |
| `READ_CONTACTS` | The "known contacts" rule, contact names in lists, and the contact picker. |
| `READ_PHONE_STATE` | Detects that you answered a ringing call, which feeds the "answered recently" rule. |
| `POST_NOTIFICATIONS` | Optional notification when a call is rejected outright (Android 13+). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Lets the app ask you to exempt it from battery optimization. It isn't a wakelock. |
| `ANSWER_PHONE_CALLS` | Declared in the manifest but not currently used by any code; the app never answers or ends calls. |

The call-screening role is granted through a system dialog, not a permission prompt.

## Privacy

All rules, lists and logs live in a local database on your phone. The app has no network
permission and no analytics. Details in [PRIVACY.md](PRIVACY.md).

## Contributing

Bug reports and pull requests are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md). For how
the app works and why, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md); for setup and testing, see
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) and [docs/TESTING.md](docs/TESTING.md).

## Security

Please report vulnerabilities privately; see [SECURITY.md](SECURITY.md).

## License

[Apache License 2.0](LICENSE)
