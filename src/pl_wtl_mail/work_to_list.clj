(ns pl-wtl-mail.work-to-list
  (:require [clojure.java.jdbc :as jdbc]
            [yesql.util :refer [slurp-from-classpath]]))

(def -work-to-list-sql
  (slurp-from-classpath "pl_wtl_mail/queries/work_to_list.sql"))

(defn production-line-id
  "Returns the Production Line ID from the given query result row."
  [r]
  (nth r (- (count r) 4)))

(defn production-line-description
  "Returns the Production Line description from the given query result row."
  [r]
  (nth r (- (count r) 3)))

(defn replace-nils-with-empty-strings [r]
  (vec (map (fn [v] (if (nil? v) "" v)) r)))

(defn off-plan?
  "Returns true if the given query result row is flagged as being ‘off plan’."
  [r]
  (= "Y" (last r)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn work-to-list
  "Returns the entire current Work To List result set as a collection of
  row arrays."
  [db]
  (jdbc/query
    db -work-to-list-sql
    {:as-arrays? true
     :row-fn replace-nils-with-empty-strings
     :result-set-fn #(doall (rest %))}))

(defn production-line
  "Returns the Production Line from the given Work To List query result
  row.

      {:id ..production line ID..
       :description ..production line description..}
  "
  [r]
  {:id (production-line-id r)
   :description (production-line-description r)})

(defn sort-by-op-dates
  "Sorts the given Work To List by the operation start and finish dates."
  [wtl]
  (sort-by
   #(->> % (drop 2) (take 2))
   (fn [[xs xe] [ys ye]]
     (let [cs (compare xs ys)]
       (if (zero? cs)
         (compare xe ye)
         cs)))
   wtl))
