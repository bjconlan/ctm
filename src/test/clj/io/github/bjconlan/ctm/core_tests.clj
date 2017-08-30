(ns io.github.bjconlan.ctm.core-tests
  (:require [clojure.test :refer :all]
            [io.github.bjconlan.ctm.core :refer :all])
  (:import (java.io FileNotFoundException)))

(deftest main-tests
  (testing "No arguments"
    [(is (= "Enter a file name (or any supported java url path)\n"
            (with-out-str (-main))))])

  (testing "No file"
    [(are [args]
       (thrown-with-msg? FileNotFoundException #"no-file-with-this-name" (apply -main args))
       ["no-file-with-this-name"]
       ["no-file-with-this-name" "or-this-name"])])

  ; Classpath resources aren't resolving in lein as expected so using relative root path
  (testing "Valid file data"
    [(are [file] (= [nil nil] (-main (str "src/test/resources/valid/" file)))    ; Dirty little hack to validate branch
                 "sample.txt"
                 "max-talks.txt"
                 "session-max.txt"
                 "session-min.txt"
                 "fail-bfd-constraints.txt")])

  (testing "Invalid data format"
    [(is (= "Unable to locate duration identifier from Invalid talk format"
            (first (.split (with-out-str (-main "src/test/resources/invalid/bad-format.txt")) "\n"))))])

  (testing "Invalid file data"
    [(are [file assert-message]
       (thrown-with-msg? AssertionError (re-pattern assert-message) (-main (str "src/test/resources/invalid/" file)))
       "session-overflow.txt" "All talk durations must be less than schedule duration"
       "session-underfill.txt" "At least one talk for each session must be specified"
       "session-underflow.txt" "All talk durations must meet the minimum schedule duration")])

  ; TODO testing the branch consisting of the failed case "Unfortunately scheduling these talks was unsuccessful"
  ;      would love to hear some suggestions as to how this could be done (again overloading rand, passing in
  ;      min and max temperatures to insure the anneal process doesn't get enough time to optimize etc?
  )