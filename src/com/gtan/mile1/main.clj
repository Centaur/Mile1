(ns com.gtan.mile1.main
  (:require [com.gtan.mile1.manifest-reader]
            [com.gtan.mile1.wizard :as wizard]
            [com.gtan.mile1.sbt :as sbt]
            [com.gtan.mile1.i18n :as i18n]
            [com.gtan.mile1.common :as common]
            [com.gtan.mile1.prj :as project]
            [clojure.tools.cli :as cli])
  (:gen-class)
  (:import (java.nio.file Files)))

(def usage (i18n/msg "main.usage"))

(def ^:private const
  (when-not *compile-files*
    {:mile1-jar-path (common/build-path (System/getProperty "user.home") ".mile1" "mile1.jar")
     :mile1-jar-url  "https://github.com/Centaur/Mile1/raw/master/downloads/mile1.jar"
     }))

(defn install-mile1-jar-if-none-installed []
  (when-not (common/exists? (const :mile1-jar-path))
    (common/download-url-to (const :mile1-jar-url)
                            (const :mile1-jar-path))))

(defn ^:deprecated do-reset
  "Delete all mile1 files except mile1 shell script, delete all sbt files. Get clean state."
  []
  (install-mile1-jar-if-none-installed)
  (Files/deleteIfExists (const :mile1-jar-path))
  (sbt/reset)
  (println "Mile1已清零."))

(defn opts-for [subcmd]
  (case subcmd
    "available" [["-a" "--all" (i18n/msg "main.show_all_versions")]]
    []))

(defn error-msg [errors]
  (str (i18n/msg "main.command_line_argument_error") (clojure.string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [subcmd (first args)
        {:keys [options errors summary arguments]} (cli/parse-opts (rest args) (opts-for subcmd))]
    (cond
      errors (exit 1 (error-msg errors)))
    (case subcmd
      "list" (sbt/show-current-installed-versions)
      "available" (sbt/show-remote-versions (:all options))
      "install" (sbt/install (or (first arguments) "latest"))
      "uninstall" (sbt/uninstall (first arguments))
      "cleanup" (sbt/cleanup)
      "use" (sbt/use-version (first arguments))
      (println usage))))

