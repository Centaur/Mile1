(ns com.gtan.mile1.sbt
  (:require [clojure.java.io :as io]
            [com.gtan.mile1.wizard :as wizard]
            [com.gtan.mile1.common :as common]
            [com.gtan.mile1.manifest-reader :as manifest-reader]
            [clojure.string :as string])
  (:import (java.nio.file Paths Files Path FileSystem)
           (java.io File)))

(def ^:private
  const (when-not *compile-files*
          (let [base-path (common/build-path (System/getProperty "user.home")
                                             ".mile1" "sbt_install")
                mile1-script-path (common/build-path (System/getProperty "mile1.script.path")) ; this is where we put sbt shell script and symbolic link to the current used sbt-launch.jar
                ]

            {:sbt-launcher-index-page     "http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/"
             :sbt-script-url              (let [non-windows "http://git.oschina.net/43284683/Mile1/raw/master/downloads/sbt"]
                                            (if (common/is-windows)
                                                  (str non-windows ".bat")
                                                  non-windows))
             :link-extractor              #"<a href=\"(\d+.*)/\"+>\1/</a>"
             :version-extractor           #"(\d+)\.(\d+)\.(\d+)(-(.*))?"
             :type-priority               {:M 1, :Beta 2, :RC 3, :GA 4}
             :installation-base-path      base-path
             :sbt-script-file-path        (let [non-windows (.resolve mile1-script-path "sbt")]
                                            (if (common/is-windows)
                                              (str non-windows ".bat")
                                              non-windows))
             :sbt-launcher-link-file-path (.resolve mile1-script-path "sbt-launch.jar")
             })))

(when-not *compile-files* (common/mkdir (const :installation-base-path)))

(defn ^:private parse-extra
  "M3 => [:M 3]
   nil => [:GA 0]"
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

(defn ^:private str->version
  "2.10.3-M3 => [[2 10 3] [:M 3]]
   2.10.3 => [[2 10 3] [:GA 0]]"
  [^String version-str]
  (let [[[_ l1 l2 l3 _ extra]] (re-seq (const :version-extractor) version-str)]
    [(vec (map #(Integer/valueOf %) [l1 l2 l3])) (parse-extra extra)]))

(defn version->str [version]
  (let [major (apply str (string/join "." (first version)))
        [_ [type-tag number]] version]
    (if (= type-tag :GA)
      (if (zero? number)
        major
        (str major "-" number))
      (str major "-" (name type-tag) number))))

(defn compare-version
  "return v1 <= v2"
  [[main1 [type1 number1]] [main2 [type2 number2]]]
  (let [priorities (:type-priority const)]
    (if (= main1 main2)
      (if (= type1 type2)
        (<= number1 number2)
        (<= (type1 priorities) (type2 priorities)))
      (<= (compare main1 main2) 0))))

(def remote-versions
  (delay
    (let [page (slurp (:sbt-launcher-index-page const))]
      (remove (fn [[_ [type-tag _]]] (nil? type-tag))
              (map (comp str->version second)
                   (re-seq (:link-extractor const) page))))))

(defn versions-by-type [tag]
  (let [filter-fn (fn [[_ [type-tag _]]] (= type-tag tag))]
    (filter filter-fn @remote-versions)))

(defn show-remote-versions [^Boolean all]
  (doseq [version (sort (comparator compare-version)
                        (if all
                          @remote-versions
                          (versions-by-type :GA)))]
    (println (version->str version))))

(defn find-latest [[head & tail]]
  (let [find-max (fn [vs accu]
                   (if (empty? vs)
                     accu
                     (let [h2 (first vs)]
                       (recur (rest vs)
                              (if (compare-version (str->version accu)
                                                   (str->version h2))
                                h2
                                accu)))))]
    (find-max tail head)))


(defn actual-version-str [^String literal-version]
  (let [version-strs (map version->str (versions-by-type :GA))]
    (case literal-version
      "latest" (find-latest version-strs) ; in
      "choose" (wizard/ask {:prompt  "选择一个版本安装"
                            :options version-strs
                            :format  :indexed ; or :text
                            :default (last version-strs)})
      literal-version)))


(defn installed-sbt-versions
  "return sorted"
  []
  (let [root (const :installation-base-path)
        stream (Files/newDirectoryStream root)
        subdirs (filter #(and (common/is-dir? %)
                              (common/exists? (.resolve % "sbt-launch.jar"))) stream)]
    (sort (comparator compare-version)
          (map (comp str->version common/path-simple-name) subdirs))))

(defn url-of-version-str [^String version-str]
  (str (const :sbt-launcher-index-page) "/" version-str "/sbt-launch.jar"))

(defn path-of-version [^String version-str]
  (.resolve (:installation-base-path const) (common/build-path version-str "sbt-launch.jar")))

(defn install
  "install specified version of sbt-launch.jar if not installed"
  [^String literal-version]
  (let [version-str (actual-version-str literal-version)
        link-file-path (:sbt-launcher-link-file-path const)
        script-file-path (:sbt-script-file-path const)]
    (if ((set (installed-sbt-versions)) (str->version version-str))
      (println "版本 " version-str " 已经安装. 退出.")
      (do
        (println "安装 sbt 版本" version-str)
        (common/download-url-to (url-of-version-str version-str)
                                (path-of-version version-str))
        (when (not (common/exists? link-file-path))
          (common/ln-replace link-file-path (path-of-version version-str)))
        (when (not (common/exists? script-file-path))
          (common/download-url-to (:sbt-script-url const)
                                  script-file-path)
          (when-not (common/is-windows)
            (common/set-executable script-file-path)))
        (println "安装完成.")))))

(defn install-if-none-installed []
  (if (empty? (installed-sbt-versions))
    (do
      (println "sbt 未安装.")
      (install "choose"))))

(def using-version-str
  (delay (let [link-file-path (const :sbt-launcher-link-file-path)]
           (manifest-reader/read-sbt-version link-file-path))))


(defn show-current-installed-versions []
  (println "已安装的版本:")
  (doseq [version-str (map version->str (installed-sbt-versions))]
    (print version-str)
    (when (= @using-version-str version-str)
      (print "(正在使用)"))
    (println)))


(defn set-using-version [^String version-str]
  (if (= @using-version-str version-str)
    (println "正在使用 sbt" version-str)
    (do (common/ln-replace (:sbt-launcher-link-file-path const)
                           (path-of-version version-str))
        (println "使用 sbt" version-str))))

(defn uninstall [^String version-str]
  (let [launcher-file-path (.resolve (const :installation-base-path)
                                     (common/build-path version-str "sbt-launch.jar"))]
    (if ((set (installed-sbt-versions)) (str->version version-str))
      (do
        (when (= @using-version-str version-str)
          (Files/delete (:sbt-launcher-link-file-path const)))
        (print "正在删除版本" version-str "...")
        (Files/delete launcher-file-path)
        (Files/delete (.getParent launcher-file-path))
        (println " 完成")
        (if-let [installed (not-empty (installed-sbt-versions))]
          (set-using-version (version->str (last installed)))))
      (do (println "版本" version-str "并未安装。")))))


(defn use-version [^String version] ; ask use if version is nil
  (install-if-none-installed)
  (let [versions (installed-sbt-versions)]
    (if (nil? version)
      (do
        (println "当前使用的版本:" @using-version-str)
        (set-using-version (wizard/ask {:prompt  "选择要使用的版本"
                                        :options versions
                                        :format  :indexed
                                        :default @using-version-str})))
      (if ((set versions) (str->version version))
        (set-using-version version)
        (println "版本 " version "未安装")))))

(defn cleanup
  "keep the latest installed version, delete others"
  []
  (install-if-none-installed)
  (let [all-installed (installed-sbt-versions)]
    (doseq [version (drop-last all-installed)]
      (uninstall (version->str version)))
    (set-using-version (version->str (last all-installed)))
    (println "sbt清理完毕, 仅保留已安装的最新版本.")))

(defn reset []
  (Files/deleteIfExists (const :sbt-script-file-path))
  (Files/deleteIfExists (const :sbt-launcher-link-file-path))
  (common/delete-tree (const :installation-base-path)))
