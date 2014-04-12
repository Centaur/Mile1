(ns com.gtan.mile1.sbt-version-test
  (:require [com.gtan.mile1.sbt-version :refer [parse-extra str->version version->str max-of-comparables]]
            [clojure.test :refer :all])
  (:import [com.gtan.mile1.sbt_version Extra Version Main]))

(deftest test-parse-extra
  []
  (is (= (parse-extra "M3")
         (Extra. :M 3)))
  (is (= (parse-extra "RC10")
         (Extra. :RC 10)))
  (is (= (parse-extra nil)
         (Extra. :GA 0)))
  (is (= (parse-extra "Beta3")
         (Extra. :Beta 3)))
  (is (= (parse-extra "MSERVER3")
         nil)))

(deftest test-max-of-comparables
  []
  (is (= (max-of-comparables [1 3 2])
         3))
  (is (= (max-of-comparables ["1" "3" "2"])
         "3"))
  (is (= (max-of-comparables [(Version. (Main. 2 10 1) (Extra. :GA 0))
                              (Version. (Main. 2 10 3) (Extra. :GA 0))
                              (Version. (Main. 2 10 3) (Extra. :RC 3))
                              (Version. (Main. 2 10 2) (Extra. :GA 0))])
         (Version. (Main. 2 10 3) (Extra. :GA 0))))
  )

(deftest test-parse-version []
                            (is (= (str->version "2.10.3-M3")
                                   (Version. (Main. 2 10 3) (Extra. :M 3))))
                            (is (= (str->version "2.10.3")
                                   (Version. (Main. 2 10 3) (Extra. :GA 0))))
                            (is (= (str->version "2.10.3-Beta3")
                                   (Version. (Main. 2 10 3) (Extra. :Beta 3))))
                            (is (= (str->version "2.10.3-RC10")
                                   (Version. (Main. 2 10 3) (Extra. :RC 10))))
                            )

(deftest test-version->str []
                           (is (= (version->str (str->version "2.10.3-M3"))
                                  "2.10.3-M3"))
                           (is (= (version->str (str->version "2.10.3"))
                                  "2.10.3")))

(run-tests)
