(ns com.gtan.mile1.sbt_test
  (:require [com.gtan.mile1.sbt :as sbt]
            [clojure.test :refer :all]))

(deftest test-parse-extra
  []
  (is (= (sbt/parse-extra "M3") [:M 3]))
  (is (= (sbt/parse-extra "RC10") [:RC 10]))
  (is (= (sbt/parse-extra nil) [:GA 0]))
  (is (= (sbt/parse-extra "Beta3") [:Beta 3]))
  )

(deftest test-parse-version
  []
  (is (= (sbt/parse-version "2.10.3-M3") [[2 10 3] [:M 3]]))
  (is (= (sbt/parse-version "2.10.3") [[2 10 3] [:GA 0]]))
  (is (= (sbt/parse-version "2.10.3-Beta3") [[2 10 3] [:Beta 3]]))
  (is (= (sbt/parse-version "2.10.3-RC10") [[2 10 3] [:RC 10]]))
  )

(deftest test-version-to-str
  []
  (is (= (sbt/version-to-str (sbt/parse-version "2.10.3-M3"))
         "2.10.3-M3"))
  (is (= (sbt/version-to-str (sbt/parse-version "2.10.3"))
         "2.10.3"))
  )
