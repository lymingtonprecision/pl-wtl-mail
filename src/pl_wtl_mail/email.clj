(ns pl-wtl-mail.email
  (:require [clojure.string :as string]
            [clj-time.core :as time]
            [clj-time.format :as time.format]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Date utility fns

(defn this-fortnight []
  (let [n (time/now)
        s (time/minus n (time/days (- (time/day-of-week n) 1)))
        e (time/minus (time/plus s (time/weeks 2)) (time/days 1))]
    [s e]))

(defn iso-date [d]
  (time.format/unparse (time.format/formatters :date) d))

(def humane-date-formatter
  (time.format/formatter "EEE, dd MMM ''yy"))

(defn humane-date [d]
  (time.format/unparse humane-date-formatter d))

(defn formatted-date-range
  [fmt-fn]
  (fn fmt-dr
    ([dr] (fmt-dr dr " to "))
    ([dr sep]
     (str (fmt-fn (first dr))
          sep
          (fmt-fn (last dr))))))

(def iso-date-range (formatted-date-range iso-date))
(def humane-date-range (formatted-date-range humane-date))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Email body utility fns

(defn single-production-line-body [pl period]
  (str "Please find attached the " (:description pl (:id pl pl))
       " Work To List for the period "
       (humane-date-range period)
       "."))

(defn multi-production-line-body [production-lines period]
  (str "Please find attached the Work To Lists for the following"
       " Production Lines, covering the period "
       (humane-date-range period)
       ".\n\n"
       "* "
       (->> production-lines
            (map #(:description % (:id % %)))
            sort
            (string/join "\n* "))))

(defn body [production-lines period]
  (case (count production-lines)
    0 nil
    1 (single-production-line-body (first production-lines) period)
    (multi-production-line-body production-lines period)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn message
  ([from-addr to-addr production-lines spreadsheet]
   (message from-addr to-addr production-lines spreadsheet (this-fortnight)))
  ([from-addr to-addr production-lines spreadsheet period]
   {:from from-addr
    :to to-addr
    :subject (str "Line Work To Lists for "
                  (iso-date-range period))
    :body [{:type "text/plain"
            :content (body production-lines period)}
           {:type :attachment
            :file-name (str "Production Line WTL " (iso-date-range period) ".xlsx")
            :content-type "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            :content spreadsheet}]}))
