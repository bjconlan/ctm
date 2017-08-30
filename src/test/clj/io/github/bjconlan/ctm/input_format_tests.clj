(ns io.github.bjconlan.ctm.input-format-tests
  "Provides unit tests for functions (both private and public) defined in the
  input-format namespace. Hopefully it does an adequate job of branch testing

  This is mostly just sanity checks but also outlines desired use cases for
  the reader"
  (:require [clojure.test :refer :all]
            [io.github.bjconlan.ctm.input-format :as input-format])
  (:import (java.text ParseException)))

(deftest parse-duration-tests
  (let [parse-duration #'input-format/parse-duration]
    ;; Desired cases
    (testing "'lightning' case is handled and returns a value of 5"
      [(is (= (parse-duration "lightning") 5))])
    (testing "'min' (minute) string values are parsed correctly"
      [(are [x result] (= result (parse-duration x))
                       "0min" 0
                       "1min" 1
                       "60min" 60
                       (str Integer/MAX_VALUE "min") Integer/MAX_VALUE)])
    ;; Undesired cases
    ;; NOTE this exemplifies some obvious edge cases which we don't entertain such as
    ;;      capitalization, untrimmed strings etc.
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
                "-1min"
                " 60min "
                "")])))

(deftest parse-line-tests
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
              "invalid")]))