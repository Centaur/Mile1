(ns com.gtan.mile1.main
  (:require [com.gtan.mile1.manifest-reader]
            [com.gtan.mile1.wizard :as wizard]
            [com.gtan.mile1.sbt :as sbt]
            [com.gtan.mile1.common :as common]
            [com.gtan.mile1.prj :as project]
            [clojure.tools.cli :as cli])
  (:gen-class)
  (:import (java.nio.file Files)))

(def usage "使用方法:
 mile1 list\t\t\t; 列出已安装的sbt版本
 mile1 available [-a]\t\t; 列出所有sbt版本，默认显示稳定版本，加上-a显示包括Milestone版本，RC版本在内的所有版本
 mile1 install [VERSION]\t; 安装指定版本的sbt, 默认安装最新的稳定版
 mile1 uninstall VERSION\t; 删除指定版本的sbt
 mile1 cleanup\t\t\t; 保留最新版本的sbt，删除其它版本
 mile1 use VERSION\t\t; 使用指定版本的sbt
 mile1 upgrade\t\t\t; 升级 Mile1 到最新版本
 mile1 usage\t\t\t; 显示本帮助信息")

(def ^:private const
  (when-not *compile-files*
    {:mile1-jar-path (common/build-path (System/getProperty "user.home") ".mile1" "mile1.jar")
     :mile1-jar-url  "http://git.oschina.net/43284683/Mile1/raw/master/downloads/mile1.jar"
     }))

(defn install-mile1-jar-if-none-installed []
  (when-not (common/exists? (const :mile1-jar-path))
    (common/download-url-to (const :mile1-jar-url)
                            (const :mile1-jar-path))))

(defn ^:deprecated do-reset
  "Delete all mile1 files except mile1 shell script, delete all sbt files. Get clean state."
  []
  (install-mile1-jar-if-none-installed)
  (Files/deleteIfExists (const :mile1-jar-path))
  (sbt/reset)
  (println "Mile1已清零."))

(defn opts-for [subcmd]
  (case subcmd
    "available" [["-a" "--all" "显示所有版本"]]
    []))

(defn error-msg [errors]
  (str "命令参数错误：\n\n" (clojure.string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [subcmd (first args)
        {:keys [options errors summary arguments]} (cli/parse-opts (rest args) (opts-for subcmd))]
    (cond
      errors (exit 1 (error-msg errors)))
    (case subcmd
      "list" (sbt/show-current-installed-versions)
      "available" (sbt/show-remote-versions (:all options))
      "install" (sbt/install (or (first arguments) "latest"))
      "uninstall" (sbt/uninstall (first arguments))
      "cleanup" (sbt/cleanup)
      "use" (sbt/use-version (first arguments))
      (println usage)))
  )

