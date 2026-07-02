(ns advertising-marketing.governor
  "AdvertisingMarketingGovernor — the independent safety/traceability
  layer for the ISCO-08 2431 independent advertising-and-marketing
  actor. Wired as its own `:govern` node in
  `advertising-marketing.actor`'s StateGraph, downstream of `:advise`
  — the Advisor has no notion of campaign provenance or
  unsubstantiated-claim/protected-category risk, so this MUST be a
  separate system able to reject a proposal (itonami actor pattern,
  per ADR-2607011000 / CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. campaign provenance  — the request's campaign must be
       registered.
    2. no-actuation           — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README robotics-premise: publishing a claim without substantiation
  review and targeting a protected-category audience segment always
  require human sign-off):
    3. :op :publish-unsubstantiated-claim.
    4. :op :target-protected-category.
    5. low confidence (< `confidence-floor`)."
  (:require [advertising-marketing.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:publish-unsubstantiated-claim :target-protected-category})

(defn- hard-violations [{:keys [proposal]} campaign-record]
  (cond-> []
    (nil? campaign-record)
    (conj {:rule :no-campaign :detail "未登録 campaign"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `advertising-marketing.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [campaign-record (store/campaign store (:campaign-id request))
        hard (hard-violations {:proposal proposal} campaign-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
