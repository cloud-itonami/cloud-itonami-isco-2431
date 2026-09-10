(ns advertising-marketing.store
  "SSoT for the ISCO-08 2431 independent advertising-and-marketing
  sole-proprietor actor. Store is a protocol injected into the
  `advertising-marketing.actor` StateGraph — `MemStore` is the
  default, deterministic, zero-dep backend; a Datomic/kotoba-server-
  backed implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  Domain:

    campaign — a registered marketing campaign (:campaign-id, :name)
    record   — a committed operating record under a campaign (produced
               asset, published asset, unsubstantiated-claim
               publication, protected-category targeting) — written
               ONLY via commit-record!, never mutated in place
    ledger   — an append-only audit trail of every proposal/verdict/
               disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (campaign [s campaign-id])
  (records-of [s campaign-id])
  (ledger [s])
  (register-campaign! [s campaign])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (campaign [_ campaign-id] (get-in @a [:campaigns campaign-id]))
  (records-of [_ campaign-id] (filter #(= campaign-id (:campaign-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-campaign! [s campaign]
    (swap! a assoc-in [:campaigns (:campaign-id campaign)] campaign) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:campaigns {} :records [] :ledger []} seed)))))
