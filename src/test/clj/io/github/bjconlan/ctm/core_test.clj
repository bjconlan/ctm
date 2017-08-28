(ns io.github.bjconlan.ctm.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [io.github.bjconlan.ctm.core :refer :all]))

;(deftest empty-arg
;  (testing "when no argument is specified a help message is presented"))
;(deftest invalid-arg
;  (testing "incorrect or invalid arguments display a concise error message to the user"))
;(deftest multiple-args
;  (testing "multiple arguments don't cause an error while asserting a valid first arg will still be processed"))
;(deftest valid-arg
;  (testing "when a valid file source is the first argument the application correctly returns the expected output"))

;(main (io/resource "sample.txt"))

(main [(io/resource "sample.txt")])

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
