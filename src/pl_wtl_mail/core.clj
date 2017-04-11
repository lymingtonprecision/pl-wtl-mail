(ns pl-wtl-mail.core
  (:refer-clojure :exclude [run!])
  (:require [taoensso.timbre :as log]
            [pl-wtl-mail.distribution :as distribution]
            [pl-wtl-mail.email :as email]
            [pl-wtl-mail.message-sender :refer [send-message]]
            [pl-wtl-mail.spreadsheet :as spreadsheet]
            [pl-wtl-mail.system :as system]
            [pl-wtl-mail.work-to-list :as wtl]))

(def ^:dynamic *default-from-addr* "ifs@lymingtonprecision.co.uk")

(defn print-message [msg]
  (clojure.pprint/pprint msg)
  (print \newline))

(defn select-production-lines
  "Given a map of Production Lines to sequences of their Work To Lists
  returns a sequence of `[production-line work-to-list]` tuples for
  those production lines whose ID is in `ids`."
  [pl-wtl ids]
  (let [ids (set ids)]
    (filter
      (fn [[pl wtl]] (ids (:id pl pl)))
      pl-wtl)))

(defn message-for
  [recipient pl-wtls
   & [{:keys [from to] :or {from *default-from-addr*}}]]
  (when (seq pl-wtls)
    (email/message
      from
      (or to (:email recipient))
      (map first pl-wtls)
      (spreadsheet/write-tmp-file pl-wtls))))

(defn messages
  [distribution-list work-to-list & [msg-opts]]
  (let [pl-wtls (group-by wtl/production-line work-to-list)]
    (->> distribution-list
         (map
          (fn [recipient]
            (when-let [recipient-wtls (select-production-lines
                                       pl-wtls
                                       (:production-lines recipient))]
              (message-for recipient recipient-wtls msg-opts))))
         (remove nil?))))

(defn overseer-message
  [to-addr work-to-list
   & [{:keys [from to] :or {from *default-from-addr*}}]]
  (when (seq work-to-list)
    (let [pl-wtls (group-by wtl/production-line work-to-list)
          pl-wtl-xlsx (spreadsheet/write-tmp-file pl-wtls)
          off-plan-xlsx (->> work-to-list
                             (filter wtl/off-plan?)
                             wtl/sort-by-op-dates
                             spreadsheet/write-off-plan-ops-tmp-file)]
      (-> (email/message
           from
           (or to to-addr)
           (map first pl-wtls)
           pl-wtl-xlsx)
          (email/add-attachment
           "Off Plan Operations.xlsx"
           off-plan-xlsx)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn run!
  [config & [{:keys [dry-run redirect overseer-only]}]]
  (let [sys (-> (system/make-system)
                (system/configure config)
                system/start)
        send! (if dry-run
                print-message
                (fn [msg]
                  (do
                   (log/info (str "emailing " (:to msg)))
                   (send-message (:email sys) msg))))]
    (try
     (let [dl (delay (distribution/list (:database sys)))
           wtl (wtl/work-to-list (:database sys))]
       (when-not overseer-only
         (doseq [msg (messages @dl wtl {:to redirect})]
           (send! msg)))
       (when-let [o (:overseer config)]
         (send!
          (overseer-message o wtl {:to redirect}))))
     (finally
      (system/stop sys)))))
