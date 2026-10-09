# 0001. No foreground or persistent service

Status: Accepted

## Context

Blocking must work reliably in the background and be battery-efficient. Those goals point in the
same direction here: `CallScreeningService` and the phone-state `BroadcastReceiver` are both woken
on demand by the system. There is nothing to poll and no idle process.

## Decision

The app runs no foreground service and holds no wakelock. Telecom binds the screening service only
when a call arrives.

## Consequences

- A foreground service would *hurt* battery for no benefit, so don't add one.
- The one real risk is OEM battery managers (Xiaomi, Samsung, Huawei and others) killing the app so
  hard that Telecom can't cold-start it for a call. This is mitigated by prompting the user (Rules
  tab) to exempt the app via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, which is neither a
  wakelock nor a foreground service.
- Because Telecom cold-starts the app, the service and receiver must get their dependencies through
  Hilt without relying on an already-running UI process.
