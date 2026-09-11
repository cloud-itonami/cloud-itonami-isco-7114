(ns concretecrew.store
  "SSoT for the ISCO-08 7114 concrete-crew job-site scheduling and
  logistics coordination actor (itonami actor pattern, ADR-2607121000
  / CLAUDE.md Actors section; README's 'Robotics premise' — a
  job-site scheduling/logistics coordination robot performs
  task/materials-usage/progress logging, crew and pour-timing
  scheduling, safety-concern surfacing and supply-order coordination
  under this advisor/governor pair, which never dispatches hardware
  itself, never performs concrete work itself, and never finalizes a
  concrete-pour/finishing-execution decision or overrides a
  site-safety officer's judgment). Modeled on
  cloud-itonami-isco-3313's accountingsupport.store and
  cloud-itonami-isco-7115's carpentry.store.

  Domain:

    client — a registered contractor/crew company (:client-id, :name)
    site   — a registered job site {:site-id :client-id :name
             :verified? boolean :max-supply-cost number}.
             `:verified?` records an INDEPENDENT verification (e.g. a
             site-safety officer's inspection sign-off) — a site
             record merely existing is not enough; it must also be
             independently verified before any coordination proposal
             may reference it.
             `:max-supply-cost` is the registered cost ceiling above
             which a `:coordinate-supply-order` proposal escalates to
             human sign-off (never a hard block on its own —
             procurement above ceiling is a judgment call, not a
             fabricated record).
    worker — a registered crew member {:worker-id :client-id :name
             :verified? boolean}, subject to the same
             registered-and-independently-verified requirement as
             site, referenced by `:schedule-crew-operation`
             proposals.
    record — a committed coordination record (logged / scheduled /
             flagged / ordered) — written ONLY via commit-record!.
             This actor never records a concrete-pour or
             finishing-execution decision itself; it only coordinates
             scheduling and logistics around one.
    ledger — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (site [s site-id])
  (worker [s worker-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s c])
  (register-site! [s si])
  (register-worker! [s w])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (site [_ site-id] (get-in @a [:sites site-id]))
  (worker [_ worker-id] (get-in @a [:workers worker-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s c]
    (swap! a assoc-in [:clients (:client-id c)] c) s)
  (register-site! [s si]
    (swap! a assoc-in [:sites (:site-id si)] si) s)
  (register-worker! [s w]
    (swap! a assoc-in [:workers (:worker-id w)] w) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :sites {} :workers {} :records [] :ledger []}
                                   seed)))))
