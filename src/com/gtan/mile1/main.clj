(ns com.gtan.mile1.main
  (:require [com.gtan.mile1.manifest-reader]
            [com.gtan.mile1.wizard :as wizard]
            [com.gtan.mile1.sbt :as sbt]
            [com.gtan.mile1.prj :as project])
  (:gen-class))

(def usage "Usage:
mile1 sbt install [VERSION|latest|choose]
mile1 sbt cleanup
mile1 sbt version
mile1 new PROJECT_NAME [as properties|scala]")

(println usage)

(defn- go "Main Program" [& args]
  (let [cmd (first args)]
    (cond
      (= cmd "sbt") (let [subcommand (second args)]
                      (cond
                        (= subcommand "install") (sbt/install (sbt/actual-version (nth args 3)))
                        (= subcommand "cleanup") (sbt/cleanup) ; keep just one latest version
                        (= subcommand "version") (sbt/show-current-installed-version)))

      (= cmd "new") (let [project-name (second args)]
                      ))))

(defn -main [& args]
  (go args))

