# cloud-itonami-isco-7114

Open Occupation Blueprint for **ISCO-08 7114**: Concrete Placers, Concrete Finishers and Related Workers.

This repository designs a forkable OSS business for an independent concrete-crew coordination practice: a job-site scheduling and logistics coordination robot manages work records, crew scheduling, safety-concern surfacing and supply-order coordination under a governor-gated actor, so the crew keeps its own operating and safety records instead of renting a closed jobsite-management SaaS.

**This actor coordinates JOB-SITE SCHEDULING/LOGISTICS ONLY. It never performs concrete work itself**, and it is structurally incapable of finalizing a concrete-pour or finishing-execution decision, or of overriding a site safety officer's judgment — those two things are hard, permanent blocks in the governor (not merely high-risk escalations), enforced by both a closed op-allowlist that simply contains no such op, and an independent scope-exclusion check over proposal text.

**Maturity: `:implemented`.** `src/concretecrew/` implements the
`ConcreteCrewActor` as a `langgraph.graph/state-graph`
(`concretecrew.actor`) wired to a `Concrete Crew Coordination Advisor`
(`concretecrew.advisor`) and an independent `ConcreteCrewGovernor`
(`concretecrew.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 25 tests green (`kbb -M:test`).

HARD invariants (always hold, never overridable — including by human
approval, since a hard block never reaches `:request-approval`):

1. **Client provenance** — the contractor/crew company must be registered.
2. **Closed op-allowlist** — `:op` must be one of `:log-work-record`,
   `:schedule-crew-operation`, `:flag-safety-concern` or
   `:coordinate-supply-order`. No op that finalizes a concrete pour,
   finalizes finishing work, or overrides a site safety officer exists
   in this actor, by construction.
3. **No-actuation** — proposal `:effect` must be `:propose` (this actor
   never performs concrete work itself; it only coordinates scheduling
   and logistics around it).
4. **Scope exclusion** — a second, independent line of defense: any
   proposal whose text content would finalize a concrete-pour/
   finishing-execution decision, or override a site safety officer's
   judgment, is a hard, permanent block regardless of op, confidence
   or stake.
5. **Site provenance** — a proposal must cite a REGISTERED site
   belonging to this client.
6. **Site independent verification** — the cited site must be
   independently verified (`:verified?` true, e.g. a site-safety
   officer's sign-off) — a site record merely existing is not enough.
7. **Worker provenance/verification** — `:schedule-crew-operation`
   proposals must cite a registered, independently verified worker
   belonging to this client.

Always-escalate (human sign-off regardless of confidence, mapping this
repo's Trust Controls in [`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (a surfaced site-condition/timing/equipment
concern always goes to a human) and `:coordinate-supply-order` above
the site's registered cost ceiling.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work** — except here the robot's job is explicitly
scoped to job-site scheduling/logistics coordination, never concrete
placement or finishing itself. A job-site scheduling/logistics
coordination robot performs work-record logging, crew and pour-timing
scheduling, and supply-order coordination under an actor that proposes
actions and an independent **Concrete Crew Governor** that gates them.
The governor never dispatches hardware itself, never performs concrete
work itself, and `:high`/`:safety-critical` actions (surfaced safety
concerns, over-ceiling supply orders) always require human sign-off. A
proposal to finalize a concrete pour or override a site safety
officer's judgment is not merely high-risk — it is outside this
actor's scope entirely and is hard-blocked.

## Core Contract

```text
site work order + crew roster + materials plan
        |
        v
Concrete Crew Coordination Advisor -> Concrete Crew Governor -> log/schedule/order, or human sign-off
        |
        v
coordination proposals (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
finalize a concrete-pour/finishing-execution decision, override a site
safety officer's judgment, suppress an operating record, or disclose
sensitive data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7114`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
