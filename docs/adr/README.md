# Architecture Decision Records

Short records of significant decisions and the reasoning behind them, so they aren't re-argued
from scratch. Each record has a status: *Accepted*, or *Superseded by NNNN*.

To propose a change, copy the latest record, give it the next number, set its status, and open a PR.
Don't edit an accepted record's decision; supersede it.

| # | Decision |
|---|---|
| [0001](0001-no-foreground-service.md) | No foreground or persistent service |
| [0002](0002-no-voip-screening.md) | Screen only real SIM/carrier calls, not VoIP |
| [0003](0003-blocked-calls-merge-sources.md) | Merge two sources for blocked calls; don't request the default-dialer role |
| [0004](0004-silence-vs-reject.md) | Silence by default; incoming-call UI is out of scope |
