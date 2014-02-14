(ns com.gtan.mile1.manifest-reader
  (:import (java.io ByteArrayOutputStream BufferedInputStream ByteArrayInputStream File FileInputStream)
           (java.util.jar Manifest JarFile JarInputStream)
           (java.nio.file Path))
  (:require [clojure.java.io :as io]
            [com.gtan.mile1.common :as common]))

(defn take-until [pred coll]
  (take-while (complement pred) coll))

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

(defn read-sbt-version "从本地sbt-launch.jar中获取版本" [^Path sbt-launch-jar-path]
  (if (common/exists? sbt-launch-jar-path)
    (-> (.toFile sbt-launch-jar-path)
        FileInputStream.
        JarInputStream.
        read-manifest
        .getMainAttributes
        (.getValue "Implementation-Version"))))



