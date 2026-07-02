# cloud-itonami-isco-2431

Open Occupation Blueprint for **ISCO-08 2431**: Advertising and Marketing Professionals.

This repository designs a forkable OSS business for an independent advertising/marketing professional: a print-and-signage robot performs physical proof printing and signage installation support under a governor-gated actor, so the practice keeps its own campaign and compliance records instead of renting a closed marketing-automation SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a print-and-signage robot performs physical proof printing and signage installation support under an actor that proposes
actions and an independent **Advertising Marketing Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
publishing a claim without substantiation review, or targeting a protected-category audience segment) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
client brief + campaign scope + compliance policy
        |
        v
Campaign Advisor -> Advertising Marketing Governor -> produce/publish, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `2431`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
