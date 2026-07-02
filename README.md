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

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section, alongside `cloud-itonami-isco-6130`, `-8160`, `-2166`, `-2641`,
`-2651`, `-2652`, `-2654`, `-1219`, `-1223`, `-1330`, `-1341`, `-1349`,
`-1412`, `-1439`, `-2144`, `-2320`, `-2411` and `-2422`): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/advertising_marketing/store.cljc` — `Store` protocol +
  `MemStore`: registered campaigns, committed records, an append-only
  audit ledger.
- `src/advertising_marketing/advisor.cljc` — `Advisor` protocol;
  `mock-advisor` (deterministic, default) proposes a campaign
  operation from a request; `llm-advisor` wraps a
  `langchain.model/ChatModel` — either way the advisor only ever
  produces a `:propose`-effect proposal, never a committed record, and
  LLM parse failures always yield `confidence 0.0` (forces escalation,
  never fabricated confidence).
- `src/advertising_marketing/governor.cljc` —
  `AdvertisingMarketingGovernor/check`: a pure function, wired as its
  own `:govern` node. Hard invariants (unregistered campaign, a
  proposal whose `:effect` isn't `:propose`) always route to `:hold`.
  Escalation invariants (`:publish-unsubstantiated-claim`,
  `:target-protected-category`, or low advisor confidence) always
  route to `:request-approval` — an `interrupt-before` node that the
  graph checkpoints and only resumes on explicit human approval
  (`actor/approve!`), matching the README's robotics-premise statement
  that publishing a claim without substantiation review and targeting
  a protected-category audience segment always require human sign-off.
- `src/advertising_marketing/actor.cljc` — `build-graph`,
  `run-request!`, `approve!`: the `langgraph.graph/state-graph` wiring
  itself.

```bash
clojure -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
