# 0004. Silence by default; the incoming-call UI is out of scope

Status: Accepted

## Context

A `CallScreeningService` can only choose ALLOW, SILENCE or REJECT for a call. It does not draw the
incoming-call UI. Whether a live (non-rejected) call shows full-screen or as a plain notification is
decided by whichever app holds the default Dialer / `InCallService` role (the stock Phone app or the
OEM dialer), based on device state (locked or in use) and its own logic. Nothing set on
`CallResponse` changes that.

## Decision

- Calls that fail the allow rules are **silenced** by default: `CallResponse.setSilenceCall(true)`
  (API 29+) keeps the call visible and answerable but suppresses ringtone and vibration.
- Below API 29, silencing isn't available, so the app falls back to a full reject.
- Users can choose REJECT everywhere with a Rules-tab setting.
- The app does not try to control full-screen versus notification rendering. Doing that ("silenced
  but notification-only, still answerable") would require becoming the default dialer and rendering
  its own in-call UI; see [0003](0003-blocked-calls-merge-sources.md) for why that role is avoided.

## Tried and reverted

Rejecting silenced calls outright (`setDisallowCall` / `setRejectCall` plus
`setSkipNotification(false)`) does avoid the full-screen UI, but it also stops the call from ringing
or being answerable at all, which is not what "silence" is supposed to mean here.
