(ns user
  (:require [clojure.pprint :refer [pprint]]
            [com.stuartsierra.component :as component]
            [reloaded.repl
             :refer [system init start stop go reset reset-all]]
            [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal report]]
            [pl-wtl-mail.logging]))

(timbre/set-level! :debug)

(reloaded.repl/set-init!
  (constantly nil))
