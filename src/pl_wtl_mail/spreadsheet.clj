(ns pl-wtl-mail.spreadsheet
  (:require [clojure.java.io :as io]
            [excel-templates.build :as excel])
  (:import [org.apache.poi.ss.usermodel PrintSetup]
           [org.apache.poi.ss.util CellRangeAddress]
           [org.apache.poi.xssf.usermodel XSSFWorkbook]))

(def header-range (CellRangeAddress/valueOf "1:1"))

(defn print-setup
  [spreadsheet]
  (let [in (io/input-stream spreadsheet)
        wb (XSSFWorkbook. in)]
    (try
      (doseq [sheet (map #(.getSheetAt wb %) (range (.getNumberOfSheets wb)))
              :let [print-setup (.getPrintSetup sheet)]]
        (doto print-setup
          (.setPaperSize PrintSetup/A3_PAPERSIZE)
          (.setLandscape true)
          (.setScale 61))
        (doto sheet
          (.setRepeatingRows header-range)
          ;; select all sheets so that printing prints them all
          (.setSelected true)))
      (finally
        (.close in)))
    (with-open [out (io/output-stream spreadsheet)]
      (.write wb out))))

(defn map->sheets
  ([xs]
   (map->sheets xs identity))
  ([xs key-fn]
   (sort-by
     :sheet-name
     (map
       (fn [[k rows]]
         {:sheet-name (key-fn k)
          1 rows})
       xs))))

(defn production-line-name
  "Returns a ‘nice’, string, name for the given Production Line.

  Normally this will simple be its description, failing that its ID."
  [pl]
  (:description pl (:id pl pl)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn write-tmp-file
  "Returns a _temp_ File instance to which the Work To Lists of the
  given Production Lines have been written.

  `production-lines` must be in the same format as the return value
  of `grouped-by-production-line`."
  [production-lines]
  (let [tf (excel/create-temp-xlsx-file "pl-wtl-")]
    (excel/render-to-file
      "templates/work_to_list.xlsx"
      tf
      {"Production Line"
       (map->sheets production-lines production-line-name)})
    (print-setup tf)
    (.deleteOnExit tf)
    tf))
