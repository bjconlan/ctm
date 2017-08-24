(ns io.github.bjconlan.ctm.input-format-test
  (:require [clojure.test :refer :all]
            [io.github.bjconlan.ctm.input-format :as input-format])
  (:import (java.text ParseException)))

(deftest parse-duration-test
  (let [parse-duration #'input-format/parse-duration]
    ;; Desired cases
    (testing "'lightning' string value has a value of 5"
      [(is (= (parse-duration "lightning") 5))
       (is (= (parse-duration " lightning ") 5))])
    (testing "'min' (minute) string values"
      [(are [x result] (= result (parse-duration x))
                       "1min" 1
                       " 60min " 60
                       (str Integer/MAX_VALUE "min") Integer/MAX_VALUE)])
    ;; Undesired cases
    (testing "nil/incorrect type values"
      [(are [x] (thrown-with-msg? AssertionError #"string?" (parse-duration x))
                nil
                10
                {})])
    (testing "negative numeric string values"
      [(is (thrown-with-msg? AssertionError #"pos?" (parse-duration "0min")))])
    ;; NOTE this exemplifies some obvious edge cases which we don't entertain such as
    ;;      capitalization (as they haven't been 'formally' outlined by the task)
    (testing "overflowing, underflowing/bad conversions of numeric string values to int"
      [(are [x] (thrown? NumberFormatException (parse-duration x))
                (str Long/MAX_VALUE "min")
                (str Long/MIN_VALUE "min")
                "hooray"
                "this isn't correct"
                "50 min"
                "LIGHTING"
                "10MIN"
                "abcd10min"
                "")])))

(deftest parse-line-test
  ;; Desired cases
  (testing "correctly formed strings"
    [(are [x result] (= result (input-format/parse-line x))
                     "testing.10min" ["testing.10min" 10]
                     "Somthing basic lightning" ["Somthing basic lightning" 5]
                     "lightning" ["lightning" 5]
                     "10min" ["10min" 10])])
  ;; Undesired cases
  (testing "incorrectly formed strings (unable to resolve duration)"
    [(are [x] (thrown? ParseException (input-format/parse-line x))
              ""
              "invalid")])
  (testing "nil/incorrect type values"
    [(are [x] (thrown-with-msg? AssertionError #"string?" (input-format/parse-line x))
              nil
              10
              {})]))