(ns com.gtan.mile1.sbt-version
  (:import (clojure.lang Keyword))
  (:require [clojure.string :as string]))

(defrecord Main [^int major ^int minor ^int patch]
  Comparable
  (compareTo [this that]
    (if (zero? (compare (:major this) (:major that)))
      (if (zero? (compare (:minor this) (:minor that)))
        (compare (:patch this) (:patch that))
        (compare (:minor this) (:minor that)))
      (compare (:major this) (:major that)))))

(defrecord Extra [^Keyword type ^int number])

(def ^:private
  const (when-not *compile-files*
          {
            :type-priority     {:M 1, :Beta 2, :RC 3, :GA 4}
            :version-extractor #"(\d+)\.(\d+)\.(\d+)(-(.*))?"
            }))

(defrecord Version [^Main main ^Extra extra]
  Comparable
  (compareTo [this that]
    (let [priorities (:type-priority const)
          main1 (:main this) main2 (:main that)
          extra1 (:extra this) extra2 (:extra that)]
      (if (= (:main this) (:main that))
        (if (= (:type extra1) (:type extra2))
          (compare (:number extra1) (:number extra2))
          (compare ((:type extra1) priorities) ((:type extra2) priorities)))
        (compare main1 main2)))))

(defn max-of-comparables [comparables]
  (reduce #(if (pos? (compare %1 %2)) %1 %2) comparables))

(defn ^Extra parse-extra
  "M3 => [:M 3]
   nil => [:GA 0]
   2 => [:GA 2]"
  [^String extra]
  (let [str->num (fn [[f s]] [f (Integer/valueOf s)])
        [typ s] (cond
                  (nil? extra) [:GA "0"]
                  (.startsWith extra "MSERVER") nil
                  (.startsWith extra "M") [:M (subs extra 1)]
                  (.startsWith extra "Beta") [:Beta (str "0" (subs extra 4))]
                  (.startsWith extra "RC") [:RC (subs extra 2)]
                  :else [:GA extra])]
    (and typ (Extra. typ (Integer/valueOf s)))))


(defn ^Version str->version
  "2.10.3-M3 => [[2 10 3] [:M 3]]
   2.10.3 => [[2 10 3] [:GA 0]]"
  [^String version-str]
  (let [[[_ l1 l2 l3 _ extra]] (re-seq (const :version-extractor) version-str)]
    (Version. (Main. (Integer/valueOf l1) (Integer/valueOf l2) (Integer/valueOf l3))
              (parse-extra extra))))

(defn ^String version->str [^Version version]
  (let [main (string/join "." ((juxt :major :minor :patch) (:main version)))
        type-tag (:type (:extra version))
        number (:number (:extra version))]
    (if (= type-tag :GA)
      (if (zero? number)
        main
        (str main "-" number))
      (str main "-" (name type-tag) number))))
