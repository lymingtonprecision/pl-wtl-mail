(ns pl-wtl-mail.system
  (:require [com.stuartsierra.component :as component]
            [pl-wtl-mail.components.ifs-connection :refer [make-connection]]
            [pl-wtl-mail.components.smtp-sender :refer [make-sender]]))

(defn make-system []
  (component/system-map
    :database (make-connection)
    :email (make-sender)))

(defn configure [system config]
  (merge-with merge system config))

(def start component/start)
(def stop component/stop)
