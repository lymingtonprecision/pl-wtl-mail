(ns pl-wtl-mail.components.smtp-sender
  (:require [com.stuartsierra.component :as component]
            [postal.core :as postal]
            [pl-wtl-mail.message-sender :refer [IMessageSender]]))

(defrecord SmtpSender [host]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  IMessageSender
  (send-message [this msg]
    (postal/send-message {:host host} msg)))

(defn make-sender
  ([]
   (map->SmtpSender {}))
  ([host]
   (->SmtpSender host)))
