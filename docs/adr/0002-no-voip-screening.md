# 0002. Screen only real SIM/carrier calls, not VoIP

Status: Accepted

## Context

VoIP apps (WhatsApp and similar) integrate with Android as self-managed connections. Telecom can
route those to a screening service, but only if the app opts in.

## Decision

The manifest does **not** declare `android.telecom.extra.CALL_SCREENING_INCLUDE_SELF_MANAGED_CALLS`,
so Telecom never passes self-managed calls to `onScreenCall`. The service also checks
`PROPERTY_SELF_MANAGED` itself as a second layer.

## Consequences

- VoIP calls are never blocked, silenced or logged by this app.
- Do not add the manifest opt-in without an explicit product decision: it would start screening VoIP
  calls too, which users haven't asked for and which can't be tested the same way.
