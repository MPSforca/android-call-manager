# Privacy

Call Manager is designed so your call data never leaves your phone.

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

## Distribution

The project is not on the Google Play Store. If it is ever published there, this document will be
updated to match any store requirements.

## Changes

Changes to this policy appear in the git history of this file.
