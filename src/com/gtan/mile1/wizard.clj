(ns com.gtan.mile1.wizard
  (:require [clojure.string :as string]
            [com.gtan.mile1.sbt-version :as sbt-version]))

(defn ^String ask
  "show a wizard , retrieve a string from user"
  [{:keys [prompt options format default] :as wizard-config}]
  (let [version-strs (map sbt-version/version->str options)]
    (try
      (doseq [version (map-indexed #(str "[" %1 "]" %2) version-strs)]
        (println version))
      (print (str prompt (if default (str "(默认 " (.indexOf version-strs default) ")")) ":"))
      (flush)
      (let [input (read-line)]
        (if (empty? input)
          default
          (case format
            :indexed (sbt-version/version->str (nth options (Integer/parseInt input)))
            :text input)))
      (catch Exception e (ask wizard-config)))))

