(ns pl-wtl-mail.message-sender)

(defprotocol IMessageSender
  (send-message [this msg]))
