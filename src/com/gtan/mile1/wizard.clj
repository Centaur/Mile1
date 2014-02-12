(ns com.gtan.mile1.wizard
  (:require [clojure.string :as string]))

(defn ask-for-a-string
  "show a wizard , retrieve a string from user"
  [{:keys [prompt options]}]
  (let [output (str prompt (string/join "\n" options))]
    (println output)
    (let [input (read-line) index (toInt input)]
      )))
