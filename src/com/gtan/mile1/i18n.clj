(ns com.gtan.mile1.i18n
  (:import (java.util ResourceBundle)))


(def ^:private messages (ResourceBundle/getBundle "i18n"))

(defn msg [a-str] (.getString messages a-str))
