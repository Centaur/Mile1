(ns com.gtan.mile1.sbt
  (:require [clojure.java.io :as io]
            [com.gtan.mile1.wizard :as wizard]
            [com.gtan.mile1.common :as common]
            [com.gtan.mile1.manifest-reader :as manifest-reader]
            [clojure.string :as string])
  (:import (java.nio.file Paths Files Path)
           (java.io File)))

(def ^{:private true}
  const (when-not *compile-files*
          (let [base-path (common/build-path (System/getProperty "user.home")
                                             ".mile1" "sbt_install")
                mile1-script-path (common/build-path (System/getProperty "mile1.script.path")) ; this is where we put sbt shell script and symbolic link to the current used sbt-launch.jar
                ]

            {:sbt-launcher-index-page     "http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/"
             :sbt-script-url              "http://git.oschina.net/43284683/Mile1/raw/master/downloads/sbt"
             :link-extractor              #"<a href=\"(\d+.*)/\"+>\1/</a>"
             :version-extractor           #"(\d+)\.(\d+)\.(\d+)(-(.*))?"
             :type-priority               {:M 1, :Beta 2, :RC 3, :GA 4}
             :installation-base-path      base-path
             :sbt-script-file-path        (.resolve mile1-script-path "sbt")
             :sbt-launcher-link-file-path (.resolve mile1-script-path "sbt-launch.jar")
             })))

(when-not *compile-files* (common/mkdir (const :installation-base-path)))

(defn parse-extra
  "M3 => [:M 3]
   nil => [:GA 0]
  "
  [extra]
  (->
    (cond
      (nil? extra) [:GA 0]
      (.startsWith extra "MSERVER") nil
      (.startsWith extra "M") [:M (subs extra 1)]
      (.startsWith extra "Beta") [:Beta (str "0" (subs extra 4))]
      (.startsWith extra "RC") [:RC (subs extra 2)]
      :else [:GA extra]
      )
    (#(vector (first %) (and % (Integer/valueOf (second %)))))))

(defn parse-version
  "2.10.3-M3 => [[2 10 3] [:M 3]]
   2.10.3 => [[2 10 3] [:GA 0]]"
  [version]
  (let [[[_ l1 l2 l3 _ extra]] (re-seq (const :version-extractor) version)]
    [(vec (map #(Integer/valueOf %) [l1 l2 l3])) (parse-extra extra)]))

(defn compare-version [v1 v2]
  (let [[main1 [type1 number1]] (parse-version v1)
        [main2 [type2 number2]] (parse-version v2)]
    (if (= main1 main2)
      (if (= type1 type2)
        (<= number1 number2)
        (<= (type1 (const :type-priority)) (type2 (const :type-priority))))
      (<= (compare main1 main2) 0))))

(def stable-versions
  (delay
    (let [page (slurp (const :sbt-launcher-index-page))
          versions (map (comp parse-version second) (re-seq (const :link-extractor) page))
          is_stable (fn [[_ [type-tag _]]] (= type-tag :GA))]
      (filter is_stable versions))))

(defn retrieve-latest [versions]
  (last versions)) ; for stable-versions this is adequate, ToDo: a generally correct implementation

(defn version-to-str [version]
  (let [main (apply str (string/join "." (first version)))
        [_ [type-tag number]] version]
    (if (= type-tag :GA)
      main
      (str main "-" (name type-tag) number))))

(defn actual-version [literal-version]
  (let [versions (map version-to-str @stable-versions)]
    (case literal-version
      "latest" (retrieve-latest versions) ; in
      "choose" (wizard/ask {:prompt  "选择一个版本安装"
                            :options versions
                            :format  :indexed ; or :text
                            :default (last versions)})
      literal-version)))


(defn installed-sbt-versions []
  (let [root (const :installation-base-path)
        stream (Files/newDirectoryStream root)
        subdirs (filter #(and (common/is-dir? %)
                              (common/exists? (.resolve % "sbt-launch.jar"))) stream)]
    (map common/path-simple-name subdirs)))

(defn url-of-version [version]
  (str (const :sbt-launcher-index-page) "/" version "/sbt-launch.jar"))

(defn dest-file-path [version]
  (.resolve (const :installation-base-path) (common/build-path version "sbt-launch.jar")))

(defn install
  "install specified version of sbt-launch.jar if not installed"
  [literal-version]
  (let [version (actual-version literal-version)
        link-file-path (const :sbt-launcher-link-file-path)
        script-file-path (const :sbt-script-file-path)]
    (if (contains? (set (installed-sbt-versions)) version)
      (println "版本 " version " 已经安装. 退出.")
      (do
        (println "安装 sbt 版本" version)
        (common/download-url-to (url-of-version version)
                                (dest-file-path version))
        (when (not (common/exists? link-file-path))
          (common/ln-replace link-file-path (dest-file-path version)))
        (when (not (common/exists? script-file-path))
          (do
            (common/download-url-to (const :sbt-script-url)
                                    script-file-path)
            (common/set-executable script-file-path)))
        (println "安装完成.")))))

(defn install-if-none-installed []
  (if (empty? (installed-sbt-versions))
    (do
      (println "sbt 未安装.")
      (install "choose"))))


(defn show-current-installed-version []
  (println "已安装的版本:")
  (doseq [version (installed-sbt-versions)]
    (println version)))

(def using-version
  (delay (let [link-file-path (const :sbt-launcher-link-file-path)]
           (manifest-reader/read-sbt-version link-file-path))))

(defn set-using-version [version]
  (if (= @using-version version)
    (println "正在使用 sbt" version)
    (do (common/ln-replace (const :sbt-launcher-link-file-path)
                           (dest-file-path version))
        (println "使用 sbt" version))))

(defn use-version [version] ; ask use if version is nil
  (install-if-none-installed)
  (let [versions (installed-sbt-versions)]
    (if (nil? version)
      (do
        (println "当前使用的版本:" @using-version)
        (set-using-version (wizard/ask {:prompt  "选择要使用的版本"
                                        :options versions
                                        :format  :indexed
                                        :default @using-version})))
      (if (contains? (set versions) version)
        (set-using-version version)
        (println "版本 " version "未安装")))))

(defn cleanup
  "keep the latest installed version, delete others"
  []
  (install-if-none-installed)
  (let [all-installed (sort (comparator compare-version) (installed-sbt-versions))]
    (doseq [version (drop-last all-installed)]
      (let [launcher-file-path (.resolve (const :installation-base-path)
                                         (common/build-path version "sbt-launch.jar"))]
        (println "正在删除版本" version)
        (Files/delete launcher-file-path)
        (Files/delete (.getParent launcher-file-path))))
    (set-using-version (last all-installed))
    (println "清理完毕.")))

(defn reset []
  (Files/delete (const :sbt-script-file-path))
  (Files/delete (const :sbt-launcher-link-file-path))
  (common/delete-tree (const :installation-base-path))
  (println "Mile1已清零."))
