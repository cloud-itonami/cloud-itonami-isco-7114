# Operator Guide

## First Deployment

1. Define the operator's job-site coverage and crew-onboarding process.
2. Define the independent site-verification process (e.g. a
   site-safety officer's inspection sign-off) that must complete
   before a site's `:verified?` flag is set — do not set it without
   that independent step.
3. Define the independent worker-verification process before a
   worker's `:verified?` flag is set.
4. Run synthetic operating cases, including scope-exclusion cases
   (attempted pour finalization, attempted safety-officer override) to
   confirm they hard-block.
5. Enable human-reviewed sign-off for `:high`/`:safety-critical`
   actions (safety concerns, over-ceiling supply orders).
6. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- independent site- and worker-verification log, kept separate from
  this actor's own registration step
- safety-critical escalation path for surfaced safety concerns
- provenance for all operating records
- human review for all escalated cases
- audit export for all gated actions

## Out of Scope — Do Not Configure Around This

Operators must not attempt to configure, extend or fork this actor to
let it finalize a concrete-pour/finishing-execution decision, or to
let it override a site safety officer's judgment. Both are hard,
permanent blocks by design (closed op-allowlist + independent
scope-exclusion check) and are not tunable trust-control parameters.

## Certification

Certified operators must prove that the governor gates every
safety-critical coordination proposal, that safety-critical risks
escalate to humans, and that no configuration path exists for this
actor to finalize pour/finishing decisions or override site-safety
officer judgment.
