(ns io.github.bjconlan.ctm.bin-packing-tests
  (:require [clojure.test :refer :all]
            [io.github.bjconlan.ctm.bin-packing :as bin-packing]))

(deftest best-fit-decreasing-tests
  ; Desired cases
  (testing "Under fill"
    [(are [weights capacities results] (= results (bin-packing/best-fit-decreasing weights capacities))
                                       [1 2 3 4] [12] [[3 2 1 0]]
                                       [1 2 3 4] [6 6] [[3 1] [2 0]]
                                       [1 2 3 4] [4 4 4] [[3] [2 0] [1]])])
  (testing "Exact fill"
    [(are [weights capacities results] (= results (bin-packing/best-fit-decreasing weights capacities))
                                       [1 2 3 4] [10] [[3 2 1 0]]
                                       [1 2 3 4] [5 5] [[3 0] [2 1]]
                                       [1 2 3 4] [4 2 4] [[3] [1] [2 0]]
                                       [1 2 3 4] [1 2 3 4] [[0] [1] [2] [3]]
                                       [0 0 0] [0 0] [[0 1 2] []])])
  (testing "Overflow"
    [(are [weights capacities results] (= results (bin-packing/best-fit-decreasing weights capacities))
                                       [1 2 3 4] [8] [[3 2 0]]
                                       [1 2 3 4] [4 4] [[3] [2 0]]
                                       [1 2 3 4] [1 1] [[0] []])])
  ; Undesired cases
  (testing "Negative weightings"
    [(are [weights capacities] (thrown-with-msg? AssertionError #"every?" (bin-packing/best-fit-decreasing weights capacities))
                               [-1] [0]
                               [0] [-1])]))