(ns com.gtan.mile1.manifest-reader
  (:import (java.io ByteArrayOutputStream BufferedInputStream ByteArrayInputStream File FileInputStream)
           (java.util.jar Manifest JarFile JarInputStream))
  (:require [clojure.java.io :as io]))

(defn take-until [pred coll]
  (take-while (complement pred) coll))

(def base-url "http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/")

(defn getBytes "从jdk JarInputStream.java中移植过来的。读取当前项的数据" [is]
  (let [buffer (make-array Byte/TYPE 8192) bao (ByteArrayOutputStream. 2048)]
    ;imperative version
    (loop []
      (let [n (.read is buffer 0 (count buffer))]
        (if-not (= n -1)
          (do
            (.write bao buffer 0 n)
            (recur)))))
    ;functional version
    (->> (repeatedly #(let [n (.read is buffer 0 (count buffer))]
                       (if-not (= n -1)
                         (.write bao buffer 0 n))))
         (take-until nil?)
         dorun)
    (.toByteArray bao))
  )

(defn find-manifest "从jar中找到Manifest.MF项" [jis]
  (->> (repeatedly #(.getNextJarEntry jis))
       (filter #(.equalsIgnoreCase JarFile/MANIFEST_NAME (.getName %)))
       first))

(defn read-manifest "从Manifest.MF中读取数据" [jis]
  (let [man (Manifest.) entry (find-manifest jis) bytes (getBytes (BufferedInputStream. jis))]
    (.read man (ByteArrayInputStream. bytes))
    (.closeEntry jis)
    man))

#_(def sbt-launch-jar-path (str (System/getProperty "user.home") "/bin/sbt-launch.jar"))

(defn read-sbt-version "从本地sbt-launch.jar中获取版本" [sbt-launch-jar-path]
  (if (.exists (io/as-file sbt-launch-jar-path))
    (-> sbt-launch-jar-path
        FileInputStream.
        JarInputStream.
        read-manifest
        .getMainAttributes
        (.getValue "Implementation-Version"))))

(defn extract-versions "从html文本中提取各个版本" [str]
  (let [extractor #"<a href=\"(\d+.*)/\"+>\1/</a>"]
    (map second (re-seq extractor str))
    ))

(defn load-data "读取测试数据" []
  (slurp base-url))

(def version-extractor #"(\d+)\.(\d+)\.(\d+)(-(.*))?")
(defn parse-version "" [version]
  (let [[[_ l1 l2 l3 _ extra]] (re-seq version-extractor version)]
    [(vec (map #(Integer/valueOf %) [l1 l2 l3])) (or extra "0")]))

(defn parse-extra [extra]
  (->
    (cond
      (.startsWith extra "M") [:M (subs extra 1)]
      (.startsWith extra "Beta") [:Beta (str "0" (subs extra 4))]
      (.startsWith extra "RC") [:RC (subs extra 2)]
      :else [:GA extra])
    (#(vector (first %) (Integer/valueOf (second %))))
    ))

(def type-prios {:M 1, :Beta 2, :RC 3, :GA 4})
(defn compare-version [v1 v2]
  (let [[main1 extra1] (parse-version v1) [main2 extra2] (parse-version v2)
        [type1 number1] (parse-extra extra1) [type2 number2] (parse-extra extra2)]
    ;    (prn [main1 extra1] [main2 extra2] [type1 number1] [type2 number2])
    (if (= main1 main2)
      (if (= type1 type2)
        (<= number1 number2)
        (<= (type1 type-prios) (type2 type-prios)))
      (<= (compare main1 main2) 0))
    ))

(defn available-versions []
  (sort compare-version (extract-versions (load-data))))

(defn newer-versions [available current]
  (if current
    (do
      (println "current version: " current)
      (drop-while #(compare-version % current) available))
    (do
      (println "sbt-launch.jar not found. Show all versions.")
      available)
    )
  )

(defn download-sbt-launch-jar [version sbt-launch-jar-path]
  (let [temp  (File/createTempFile "sbt-launch.jar" "tmp")
        sbt-jar-file (clojure.java.io/as-file sbt-launch-jar-path)]
    (with-open [is (clojure.java.io/input-stream (str base-url "/" version "/sbt-launch.jar"))
                os (clojure.java.io/output-stream temp)]
      (clojure.java.io/copy is os))
    (if (.exists sbt-jar-file) (clojure.java.io/delete-file sbt-jar-file))
    (.renameTo temp (clojure.java.io/as-file sbt-launch-jar-path))))

#_(defn update-sbt []
  (let [versions (newer-versions (available-versions) (current-sbt-version))]
    (dorun (map-indexed #(println (format "[%d]\t%s" %1 %2)) versions))
    (print "Select a version to install [0]") (flush)
    (let [v (Integer/valueOf (str "0" (read-line))) selected (nth versions v)]
      (println "Downloading sbt-launch.jar of version " selected)
      (download-sbt-launch-jar selected)
      (println "Finished"))))



