(defproject Mile1 "0.1.0-SNAPSHOT"
  :description "What sbt should've done but unfortunately not"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :resource-paths ["resources"]
  :main com.gtan.mile1.main
  :aot [com.gtan.mile1.main]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.3.1"]]
  :target-path "downloads/"
  :jar-name "mile1-nodep.jar"
  :uberjar-name "mile1.jar"
  ;:plugins [[lein-autoreload "0.1.0"]]
  )
