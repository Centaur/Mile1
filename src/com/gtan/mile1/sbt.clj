(ns com.gtan.mile1.sbt
  (:import (java.nio.file Paths)
           (clojure.lang Keyword))
  (:require [clojure.java.io :as io]
            [com.gtan.mile1.wizard :as wizard]
            [com.gtan.mile1.common :as common]
            [com.gtan.mile1.manifest-reader :as manifest-reader]
            [com.gtan.mile1.sbt-version :as sbt-version :refer [version->str str->version]]
            [clojure.string :as string])
  (:import (java.nio.file Paths Files Path FileSystem)
           (java.io File)
           (com.gtan.mile1.sbt_version Version)))

(def ^:private
  const (when-not *compile-files*
          (let [base-path (common/build-path (System/getProperty "user.home")
                                             ".mile1" "sbt_install")
                mile1-script-path (common/build-path (System/getProperty "mile1.script.path" "/nonexist")) ; this is where we put sbt shell script and symbolic link to the current used sbt-launch.jar
                ]

            {:sbt-launcher-index-page     "http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/"
             :sbt-script-url              (let [non-windows "http://git.oschina.net/43284683/Mile1/raw/master/downloads/sbt"]
                                            (if (common/is-windows)
                                              (str non-windows ".bat")
                                              non-windows))
             :link-extractor              #"<a href=\"(\d+.*)/\"+>\1/</a>"
             :installation-base-path      base-path
             :sbt-script-file-path        (if (common/is-windows)
                                            (.resolve mile1-script-path "sbt.bat")
                                            (.resolve mile1-script-path "sbt"))
             :sbt-launcher-link-file-path (.resolve mile1-script-path "sbt-launch.jar")
             })))

(when-not *compile-files* (common/mkdir (const :installation-base-path)))

(def remote-versions
  "return lazy Seq[Version]"
  (delay
    (let [page (slurp (:sbt-launcher-index-page const))]
      (remove #(nil? (:type (:extra %)))
              (map (comp str->version second)
                   (re-seq (:link-extractor const) page))))))

(defn versions-by-type
  "return Seq[Version]"
  [^Keyword tag]
  (filter #(= tag (:type (:extra %))) @remote-versions))

(defn show-remote-versions [^Boolean all]
  (doseq [version (sort (if all
                          @remote-versions
                          (versions-by-type :GA)))]
    (println (version->str version))))

(defn find-latest [[head & tail]]
  (let [find-max (fn [vs accu]
                   (if (empty? vs)
                     accu
                     (let [h2 (first vs)]
                       (recur (rest vs)
                              (if (<= (compare (str->version accu)
                                               (str->version h2))
                                      0)
                                h2
                                accu)))))]
    (find-max tail head)))


(defn actual-version-str [^String literal-version]
  (let [versions (versions-by-type :GA)
        version-strs (map version->str versions)
        latest (version->str (sbt-version/max-of-comparables versions))
        ]
    (case literal-version
      "latest" latest                                       ; in
      "choose" (wizard/ask {:prompt  "选择一个版本安装"
                            :options version-strs
                            :format  :indexed               ; or :text
                            :default latest})
      literal-version)))


(defn installed-sbt-versions
  "return sorted Seq[Version]"
  []
  (let [root (const :installation-base-path)
        stream (Files/newDirectoryStream root)
        subdirs (filter #(and (common/is-dir? %)
                              (common/exists? (.resolve % "sbt-launch.jar"))) stream)]
    (sort (map (comp str->version common/path-simple-name) subdirs))))

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
        (when-not (common/exists? link-file-path)
          (common/ln-replace link-file-path (path-of-version version-str)))
        (when-not (common/exists? script-file-path)
          (common/download-url-to (:sbt-script-url const)
                                  script-file-path)
          (when-not (common/is-windows)
            (common/set-executable script-file-path)))
        (println "安装完成.")))))

(defn install-if-none-installed []
  (when (empty? (installed-sbt-versions))
    (println "sbt 未安装.")
    (install "choose")))

(def using-version-str
  (delay (let [link-file-path (const :sbt-launcher-link-file-path)]
           (manifest-reader/read-sbt-version link-file-path))))


(defn show-current-installed-versions []
  (println "已安装的版本:")
  (doseq [version-str (map sbt-version/version->str (installed-sbt-versions))]
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
          (set-using-version (sbt-version/version->str (last installed)))))
      (do (println "版本" version-str "并未安装。")))))


(defn use-version [^String version]                         ; ask use if version is nil
  (install-if-none-installed)
  (let [versions (installed-sbt-versions)]
    (if-not version
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
      (uninstall (sbt-version/version->str version)))
    (set-using-version (sbt-version/version->str (last all-installed)))
    (println "sbt清理完毕, 仅保留已安装的最新版本.")))

(defn reset []
  (Files/deleteIfExists (const :sbt-script-file-path))
  (Files/deleteIfExists (const :sbt-launcher-link-file-path))
  (common/delete-tree (const :installation-base-path)))
