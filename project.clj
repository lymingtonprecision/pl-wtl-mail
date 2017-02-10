(defproject lymingtonprecision/pl-wlt-mail "1.1.3-SNAPSHOT"
  :description "LPE Production Line Work To List Mailer"
  :url "https://github.com/lymingtonprecision/pl-wtl-mail"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; configuration
                 [aero "1.0.0"]

                 ;; cli
                 [org.clojure/tools.cli "0.3.5"]

                 ;; general system libs
                 [com.stuartsierra/component "0.3.1"]
                 [clj-time "0.12.0"]

                 ;; database
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.clojars.zentrope/ojdbc "11.2.0.3.0"]
                 [yesql "0.5.3"]

                 ;; excel
                 [com.infolace/excel-templates "0.3.3"]

                 ;; email
                 [com.draines/postal "2.0.1"]

                 ;;;; logging
                 ;; use logback as the main Java logging implementation
                 [ch.qos.logback/logback-classic "1.1.7"]
                 [ch.qos.logback/logback-core "1.1.7"]
                 ;; with SLF4J as the main redirect
                 [org.slf4j/slf4j-api "1.7.21"]
                 [org.slf4j/jcl-over-slf4j "1.7.21"]
                 [org.slf4j/log4j-over-slf4j "1.7.21"]
                 [org.apache.logging.log4j/log4j-to-slf4j "2.6.2"]
                 ;; and timbre for our own logging
                 [com.taoensso/timbre "4.7.3"]]

  :main pl-wtl-mail.main
  :aot [pl-wtl-mail.main]

  :profiles
  {:dev {:dependencies [[reloaded.repl "0.2.2"]]
         :source-paths ["dev"]}}

  :repl-options {:init-ns user}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["change" "version"
                   "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
