(ns pl-wtl-mail.main
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [pl-wtl-mail.logging]
            [pl-wtl-mail.config :as config]
            [pl-wtl-mail.core :as pl-wtl-mail])
  (:gen-class))

(def cli-opts
  [["-c" "--config PATH"
    "Path to EDN configuration file"
    :validate [#(.canRead (clojure.java.io/file %))
               "Must be a valid, readable, file path"]]
   ["-u" "--user USERNAME"
    "The IFS database user account to use"]
   ["-p" "--password PASSWORD"
    "The database password"]
   [nil "--dry-run"
    "Don't send emails, just print them"]
   ["-r" "--redirect EMAIL_ADDR"
    "Re-direct all emails to this address"]
   ["-h" "--help"
    "Print this usage summary"]])

(defn usage-msg [opt-summary]
  (->> ["Production Line Work To List Mailer"
        ""
        "Extracts the prioritized work to lists for the current fortnight"
        "from IFS and emails them to the Production Line Team Leaders and"
        "Production Supervisors."
        ""
        "Note that a database username and password combination *must* be"
        "supplied. Either as environment variables, command line options,"
        "or configuration file entries."
        ""
        "Usage: java -jar pl-wtl-mail.jar [options]"
        ""
        "Supported environment variables:"
        ""
        "  DATABASE_USER      The IFS database user account to use"
        "  DATABASE_PASSWORD  The password for the user account"
        ""
        "Supported command line options:"
        ""
        opt-summary]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following options are invalid:\n\n"
       (string/join \newline errors)))

(defn exit! [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-opts)
        errors (concat
                 (or errors [])
                 (map #(str "Unrecognised option: " %) arguments))]
    (cond
      (:help options) (exit! 0 (usage-msg summary))
      (seq errors) (exit! 1 (error-msg errors)))
    (let [config (config/from
                   [(config/default-path)
                    (:config options)
                    {:database {:user (:user options)
                                :password (:password options)}}])]
      (pl-wtl-mail/run! config options))))
