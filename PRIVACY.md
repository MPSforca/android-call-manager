# Call Manager Privacy Policy

_Last updated: 2026-11-08_

Call Manager ("the app") is an open-source Android app that blocks unwanted phone calls. It is
designed so your call data never leaves your phone. The developer does not collect, receive, store,
share or sell any personal data.

## What the app accesses

- **Call log** — to show your history, to list calls blocked by Android or your carrier, and to
  check whether you called a number recently.
- **Contacts** — to recognize known callers and show names.
- **Phone state** — to notice when you answer a call.
- **Incoming call numbers and caller-ID names** — handed to the app by Android while it screens a
  call.

## What the app stores

In a local database on the device: your block list, allow list, name patterns, the callers you
answered (number + timestamp), a log of calls the app blocked (number, name, time, reason), and your
settings.

## What leaves the device

Nothing. The app declares no `INTERNET` permission, so it cannot make network requests. There are
no analytics, crash reporters, ads, or accounts.

## Android backup

The app currently leaves Android's `allowBackup` enabled. Depending on your device and settings,
Android's own backup may copy the app's local data to your Google account. That is controlled by
the system and your backup settings, not by this app. If this matters to you, disable app backup in
Android's settings.

## Removing your data

Clear the app's storage or uninstall it in Android settings. Call-log and contact data belong to
Android and are only read by this app, never modified.

## Children

The app is not directed at children and collects no information from anyone.

## Permissions

The app asks for access to your call log, contacts and phone state only for the purposes above, and
only on your device. Each permission and its purpose is listed in the
[README](https://github.com/MPSforca/android-call-manager#permissions-and-why). You can revoke any
of them in Android settings at any time; the related features then stop working.

## Open source

The complete source code is public at
[github.com/MPSforca/android-call-manager](https://github.com/MPSforca/android-call-manager), so
every statement here can be checked against the code.

## Contact

Questions about this policy: matheus.sforca2@gmail.com, or open an issue at
[github.com/MPSforca/android-call-manager/issues](https://github.com/MPSforca/android-call-manager/issues).

## Changes

Changes to this policy are published at this address, with the date above updated. The full history
is in the git log of `PRIVACY.md`.
