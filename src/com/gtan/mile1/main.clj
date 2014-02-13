(ns com.gtan.mile1.main
  (:require [com.gtan.mile1.manifest-reader]
            [com.gtan.mile1.wizard :as wizard]
            [com.gtan.mile1.sbt :as sbt]
            [com.gtan.mile1.prj :as project])
  (:gen-class))

(def usage "Usage:
mile1 install [VERSION|latest]
mile1 cleanup
mile1 use [VERSION]
mile1 new PROJECT_NAME [as properties|scala]")

(defn- go "Main Program" [args]
  (let [cmd (first args)]
    (case cmd
      "install" (sbt/install (or (second args) "choose"))
      "cleanup" (sbt/cleanup) ; keep just one latest version
      "use" (sbt/use-version (second args))
      "new" (let [project-name (second args)])
      (println (type cmd) usage))))

(defn -main [& args]
  (go args))

