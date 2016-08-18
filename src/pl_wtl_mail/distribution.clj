(ns pl-wtl-mail.distribution
  (:refer-clojure :exclude [list])
  (:require [yesql.core :refer [defqueries]]))

(defqueries "pl_wtl_mail/queries/distribution.sql")

(defn collect-production-lines-by-id
  "Given a seqeuence of results from the one of the distribution queries
  returns a collection of the unique id/email pairs and their corresponding
  production line subscriptions.

  Example:

      (collect-production-lines-by-id
        [{:id \"1\" :email \"crivera@example.com\" :production_line \"1\"}
         {:id \"1\" :email \"crivera@example.com\" :production_line \"2\"}
         {:id \"2\" :email \"skim@example.com\" :production_line \"14\"}
         {:id \"3\" :email \"jdunn@example.com\" :production_line \"21\"}
         {:id \"1\" :email \"crivera@example.com\" :production_line \"9\"}])
      ;=> [{:id \"1\" :email \"crivera@example.com\"
            :production-lines [\"1\" \"2\" \"9\"]}
           {:id \"2\" :email \"skim@example.com\" :production-lines [\"14\"]}
           {:id \"3\" :email \"jdunn@example.com\" :production-lines [\"21\"]}]
  "
  [rs]
  (vals
    (reduce
      (fn [rs r]
        (let [e (get rs (:id r)
                     {:id (:id r)
                      :email (:email r)
                      :production-lines []})]
          (assoc
            rs (:id r)
            (update e :production-lines
                    conj
                    (:production_line r)))))
      {}
      rs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn team-leader-production-lines
  "Returns a collection of the Team Leaders to whom Work To Lists should
  be distributed.

  Each entry is a map of the Team Leaders `:id`, `:email`, and a
  collection of the `:production-lines` (IDs) they should be sent."
  [db]
  (-team-leader-production-lines
    {}
    {:connection db
     :result-set-fn collect-production-lines-by-id}))

(defn supervisor-production-lines
  "Returns a collection of the Supervisors to whom Work To Lists should
  be distributed.

  Each entry is a map of the Supervisors `:id`, `:email`, and a
  collection of the `:production-lines` (IDs) they should be sent."
  [db]
  (-supervisor-production-lines
    {}
    {:connection db
     :result-set-fn collect-production-lines-by-id}))

(defn list
  "Returns a collection of all the people to whom the Work To Lists
  should be distributed."
  [db]
  (concat
    (team-leader-production-lines db)
    (supervisor-production-lines db)))
