(ns com.gtan.mile1.sbt
  (:require [clojure.java.io :as io])
  (:import (java.nio.file Paths)
           (java.io File)))

(def sbt-versions-page "http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/")

(defn parse-extra
  "M3 => [:M 3]"
  [extra]
  (->
    (cond
      (.startsWith extra "MSERVER") nil
      (.startsWith extra "M") [:M (subs extra 1)]
      (.startsWith extra "Beta") [:Beta (str "0" (subs extra 4))]
      (.startsWith extra "RC") [:RC (subs extra 2)]
      :else [:GA extra])
    (#(vector (first %) (and % (Integer/valueOf (second %)))))))

(def version-extractor #"(\d+)\.(\d+)\.(\d+)(-(.*))?")
(defn parse-version
  "2.10.3-M3 => [2.10.3-M3 2 10 3 -M3 [:M 3]]"
  [version]
  (let [[[_ l1 l2 l3 _ extra]] (re-seq version-extractor version)]
    [(vec (map #(Integer/valueOf %) [l1 l2 l3])) (parse-extra (or extra "0"))]))

(def type-prios {:M 1, :Beta 2, :RC 3, :GA 4})
(defn compare-version [v1 v2]
  (let [[main1 [type1 number1]] (parse-version v1)
        [main2 [type2 number2]] (parse-version v2)]
    (if (= main1 main2)
      (if (= type1 type2)
        (<= number1 number2)
        (<= (type1 type-prios) (type2 type-prios)))
      (<= (compare main1 main2) 0))))

(def link-extractor #"<a href=\"(\d+.*)/\"+>\1/</a>")
(def stable-versions
  (let [page (slurp sbt-versions-page)
        versions (map (comp parse-version second) (re-seq link-extractor page))
        is_stable (fn [[_ _ _ _ _ [type-tag _]]] (= type-tag :GA))]
    (delay (filter is_stable versions))))

(defn retrieve-latest-stable-version []
  (let [versions @stable-versions]
    ))

(defn list-stable-versions-and-choose)
(defn actual-version [version]
  (cond
    (= version "latest") (retrieve-latest-stable-version) ; in
    (= version "choose") (wizard/ask-for-a-string {:prompt  "选择一个版本安装"
                                                   :options @stable-versions})
    :else version))

(def installed-sbt-versions
  (delay ()))

(defn url-of-version [version]
  (str sbt-versions-page "/" version "/sbt-launch.jar"))

(def sbt-installation-base-dir "~/mile1/sbt")
(defn dest-file-path [version]
  (Paths/get sbt-installation-base-dir version "sbt-launch.jar"))

(defn download-url-to [url ^File target-file]
  (with-open [in (io/input-stream url)
              out (io/output-stream target-file)]
    (io/copy in out)))

(defn install
  "install specified version of sbt-launch.jar if not installed"
  [version]
  (if (@installed-sbt-versions version)
    (println "Version " version " already installed. Exit")
    (do
      (println "Installing sbt version " version)
      (download-url-to (url-of-version version)
                       (dest-file-path version)))))

