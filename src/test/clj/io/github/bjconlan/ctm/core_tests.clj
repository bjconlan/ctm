(ns io.github.bjconlan.ctm.core-tests
  (:require [clojure.test :refer :all]
            [io.github.bjconlan.ctm.core :refer :all])
  (:import (java.io FileNotFoundException)))

(deftest main-tests
  (testing "No arguments"
    [(is (= "Enter a file name (or any supported java url path)\n"
            (with-out-str (main []))))])

  (testing "No file"
    [(are [args]
       (thrown-with-msg? FileNotFoundException #"no-file-with-this-name" (main args))
       ["no-file-with-this-name"]
       ["no-file-with-this-name" "or-this-name"])])

  ; Classpath resources aren't resolving in lein as expected so using relative root path
  (testing "Valid file data"
    [(are [file] (= [nil nil] (main [(str "src/test/resources/valid/" file)]))    ; Dirty little hack to validate branch
                 "sample.txt"
                 "max-talks.txt"
                 "session-max.txt"
                 "session-min.txt"
                 "fail-bfd-constraints.txt")])

  (testing "Invalid data format"
    [(is (= "Failed to parse \"Invalid talk format\".It will be omitted. Unable to locate duration identifier from Invalid talk format"
            (with-out-str (main ["src/test/resources/invalid/bad-format.txt"]))))])
  (testing "Invalid file data"
    [(are [file assert-message]
       (thrown-with-msg? AssertionError (re-pattern assert-message) (main [(str "src/test/resources/invalid/" file)]))
       "session-overflow.txt" "All talk durations must be less than schedule duration"
       "session-underfill.txt" "At least one talk for each session must be specified"
       "session-underflow.txt" "All talk durations must meet the minimum schedule duration")]))