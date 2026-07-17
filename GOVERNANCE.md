# Governance

`cloud-itonami-isco-7114` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions, commit records or
  disclose records.
- Concrete Crew Governor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- the closed op-allowlist never grows to include an op that finalizes
  a concrete-pour/finishing-execution decision or overrides a site
  safety officer's judgment.
- every commit, hold and approval path is auditable.
- real client/site/worker/operator data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and data-flow
review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling client/site/worker/operator data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- attempting to widen this actor's scope into concrete
  placement/finishing execution or site-safety-officer override
