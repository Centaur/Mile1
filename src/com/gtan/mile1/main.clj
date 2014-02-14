(ns com.gtan.mile1.main
  (:require [com.gtan.mile1.manifest-reader]
            [com.gtan.mile1.wizard :as wizard]
            [com.gtan.mile1.sbt :as sbt]
            [com.gtan.mile1.common :as common]
            [com.gtan.mile1.prj :as project])
  (:gen-class)
  (:import (java.nio.file Files)))

(def usage "Usage:
mile1 install [VERSION|latest]
mile1 cleanup
mile1 use [VERSION]
mile1 new PROJECT_NAME [as properties|scala]
mile1 reset")

(def const
  (when-not *compile-files*
    {:mile1-jar-path (common/build-path (System/getProperty "user.home") ".mile1" "mile1.jar")
     :mile1-jar-url  "http://git.oschina.net/43284683/Mile1/raw/master/downloads/mile1.jar"
     }))

(defn install-mile1-jar-if-none-installed []
  (when-not (common/exists? (const :mile1-jar-path))
    (common/download-url-to (const :mile1-jar-url)
                            (const :mile1-jar-path))))

(defn do-reset
  "Delete all mile1 files except mile1 shell script, delete all sbt files. Get clean state."
  []
  (install-mile1-jar-if-none-installed)
  (Files/delete (const :mile1-jar-path))
  (sbt/reset))

(defn- go "Main Program" [args]
  (let [cmd (first args)]
    (case cmd
      "install" (sbt/install (or (second args) "choose"))
      "cleanup" (sbt/cleanup) ; keep just one latest version
      "use" (sbt/use-version (second args))
      "new" (let [project-name (second args)])
      "reset" (do-reset)
      (println (type cmd) usage))))

(defn -main [& args]
  (go args))

