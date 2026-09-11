# Contributing

`cloud-itonami-isco-7114` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:test
```

Keep changes small and include tests for policy, audit, store or scope-
exclusion behavior.

## Rules

- Do not commit real client, site, worker or operating data.
- Keep coordination writes and disclosures behind Concrete Crew Governor.
- Never widen the closed op-allowlist to include an op that finalizes a
  concrete-pour/finishing-execution decision or overrides a site
  safety officer's judgment — those must remain outside this actor's
  scope entirely, not merely high-risk.
- Treat this occupation's workflows as high-risk: add tests for
  permission, provenance/verification, scope exclusion, safety and
  audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
