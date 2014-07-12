(ns com.gtan.mile1.i18n
  (:import (java.util ResourceBundle)
           (java.text MessageFormat)))


(def ^:private messages (ResourceBundle/getBundle "i18n"))

(defn msg
  ([a-str] (.getString messages a-str))
  ([a-str & args] (-> messages
                   (.getString a-str)
                   (MessageFormat/format (to-array args)))))
