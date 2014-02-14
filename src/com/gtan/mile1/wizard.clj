(ns com.gtan.mile1.wizard
  (:require [clojure.string :as string]))

(defn ask
  "show a wizard , retrieve a string from user"
  [{:keys [prompt options format default] :as wizard-config}]
  (try
    (doseq [option (map-indexed #(str "[" %1 "]" %2) options)]
      (println option))
    (print (str prompt (if default (str "(默认 " (.indexOf options default) ")")) ":"))
    (flush)
    (let [input (read-line)]
      (if (empty? input)
        default
        (case format
          :indexed (nth options (Integer/parseInt input))
          :text input)))
    (catch Exception e (ask wizard-config))))

