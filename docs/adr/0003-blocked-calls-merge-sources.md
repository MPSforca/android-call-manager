# 0003. Merge two sources for blocked calls; don't request the default-dialer role

Status: Accepted

## Context

Users want one place to see every blocked call. Third-party apps cannot enumerate Android's native
block list (`BlockedNumberContract`); reading the full list requires the default-dialer role. That
role is a very large scope and risk (the app would have to be a full phone app) for what this
project needs.

## Decision

Every blocked call, from any source (this app, Android's own block list, the carrier, another
screening app), is tagged `CallLog.Calls.TYPE = BLOCKED_TYPE` in the *system* call log, which any
app holding `READ_CALL_LOG` can read. `BlockedCallMerger` combines that with the app's own Room log
(which has richer per-rule reasons) and dedupes by number plus a 5-second timestamp window.

The app does not request the default-dialer role.

## Consequences

- The native block list itself still can't be listed; only calls that actually happened show up.
- `BLOCK_REASON` is available only on API 29+. Older versions get a generic "Blocked by Android"
  label.
- Merge logic is a pure function and is unit tested without Android.
