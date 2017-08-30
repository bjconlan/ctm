(ns io.github.bjconlan.ctm.schedule-tests
  (:require [clojure.test :refer :all]
            [io.github.bjconlan.ctm.schedule :as schedule]))

;; Looking at these I could well need to tune the fitness test (is an overflow
;; of 1 with a constraint of max 100 worth more (better) than of max 10?
(deftest fitness-tests
  (let [fitness #'schedule/fitness]
    (testing "Adjusts based on the 'openness' of the constaints"
      [(are [session result]
         (= result (fitness session))
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 10 :max 15}} 100
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 10 :max 10}} 100
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 4 :max 10}}  22)])

    (testing "Adjusts based on the 'underflow/overflow' of the session"
      [(are [session result]
         (= result (fitness session))
         {:talks [["b" 2] ["c" 3] ["d" 4]]         :constraints {:min 10 :max 10}} 81
         {:talks [["a" 2] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 10 :max 10}} 81
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 11 :max 11}} 100
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 9 :max 9}}   64
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 4 :max 9}}   14)])

    (testing "Adjust based on overflow greater than min constraint"
      [(is (= (fitness {:talks [["a" 100]] :constraints {:min 20 :max 50}}) 30))])))

(deftest valid?-tests
  (let [valid? #'schedule/valid?]
    (testing "Asserts talks are reported correctly as valid when fulfill constraints"
      [(are [session]
         (true? (valid? session))
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 10 :max 15}}
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 10 :max 10}}
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 4 :max 10}})])

    (testing "Asserts talks are reported correctly as valid when fail constraints"
      [(are [session]
         (false? (valid? session))
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 9 :max 9}}
         {:talks [["a" 1] ["b" 2] ["c" 3] ["d" 4]] :constraints {:min 11 :max 11}}
         {:talks [] :constraints {:min 1 :max 1}})])))

(deftest move-session-talk-tests
  (let [move-session-talk #'schedule/move-session-talk]
    (testing "Moving a valid talk duration returns the updated to/from talks"
      [(are [from-session to-session talk-duration result]
         (= result (move-session-talk from-session to-session talk-duration))
         {:talks [["a" 1]]} {:talks [["b" 2]]} 1 [{:talks []} {:talks [["b" 2] ["a" 1]]}]
         {:talks [["a" 1] ["b" 1]]} {:talks [["c" 2]]} 1 [{:talks [["b" 1]]} {:talks [["c" 2] ["a" 1]]}])])

    (testing "Moving from an invalid talk duration returns the to/from session arguments unmodified"
      [(is (let [from-session {:talks [["b" 2]]}
                 to-session   {:talks [["a" 1]]}]
             (= [from-session to-session] (move-session-talk from-session to-session 1))))])

    (testing "Moving from an empty session fails"
      [(is (thrown-with-msg? AssertionError #"seq"
                             (move-session-talk {:talks []} {:talks [["a" 1]]} 0)))])))

; This is really just exercising the functions move-session-talk and fitness
; (across a small session subset)
(deftest local-search-optimum-fitness-tests
  (let [local-search-by-fitness #'schedule/local-search-by-fitness]
    ; because this uses a random selection for the session to use as the 'root'
    ; or comparator I've only provided 1 invalid session to be selected as the
    ; 'root session'
    (testing "Tests to see if an obvious best selection choice is made"
      [(is (= [{:talks [["b2" 2]], :constraints {:min 2, :max 3}}
               {:talks [["a1" 1] ["b1" 1]] :constraints {:min 2 :max 2}}]
              (local-search-by-fitness [{:talks [["a1" 1]] :constraints {:min 2 :max 2}}
                                        {:talks [["b1" 1] ["b2" 2]] :constraints {:min 2 :max 3}}])))])

    (testing "No 'invalid' sessions"
      [(is (thrown-with-msg? AssertionError #"some"
                             (local-search-by-fitness
                               [{:talks [["a1" 1]] :constraints {:min 1 :max 1}}
                                {:talks [["b1" 1]] :constraints {:min 1 :max 2}}])))])

    (testing "Single session"
      [(is (thrown-with-msg? AssertionError #"< 1"
                             (local-search-by-fitness
                               [{:talks [["a1" 1]] :constraints {:min 2 :max 2}}])))])))

; How do you test the randomization of an anneal?! COQ!? (rebind rand/rand-nth?
(deftest anneal-tests
  (let [anneal #'schedule/anneal]
    (testing "Invalid/exhausted schedules return nil"
      [(are [sessions] (nil? (anneal sessions 0 10))
                       []
                       [{:talks [["a" 2]] :constraints {:min 1 :max 1}}]
                       [{:index 0 :talks [["a1" 1]] :constraints {:min 2 :max 2}}
                        {:index 1 :talks [["a2" 1]] :constraints {:min 1 :max 1}}])])

    (testing "Valid schedules return unaltered"
      [(are [sessions] (= sessions (anneal sessions 0 10))
                       [{:talks [["a1" 1] ["b1" 1]] :constraints {:min 2 :max 2}}
                        {:talks [["a2" 1]] :constraints {:min 1 :max 1}}]
                       [{:talks [["a" 1]] :constraints {:min 1 :max 1}}])])

    (testing "Valid annealing threshold parameters"
      [(is (thrown-with-msg? AssertionError #"<=" (anneal [] 10 0)))])

    (testing "When annealing process is exhausted"
      [(is (nil? (anneal [{:index 0 :talks [["a1" 1] ["b1" 1]] :constraints {:min 1 :max 2}}
                          {:index 1 :talks [["a2" 2]] :constraints {:min 1 :max 1}}
                          {:index 2 :talks [["a3" 1]] :constraints {:min 1 :max 1}}] 0 1)))])))

; Can't do any real tests here because of how random the annealing function is
; only validate that assertions are maintained.
(deftest solve-tests
  (testing "Session constraints are valid"
    [(are [session-constraints]
       (thrown-with-msg? AssertionError #"every\? true\?" (schedule/solve [["" 1]] session-constraints))
       [{:min -1 :max 1}]
       [{:min 1 :max -1}]
       [{:min 1 :max 0}])])

  (testing "Session duration must be within max constraint range"
    [(are [talks session-constraints]
       (thrown-with-msg? AssertionError #"<= \(apply" (schedule/solve talks session-constraints))
       [["a" 1] ["b" 2]] [{:min 1 :max 2}]
       [["a" 1] ["b" 2]] [{:min 1 :max 1} {:min 1 :max 1}])])

  (testing "session duration must be within min constraint range"
    [(are [talks session-constraints]
       (thrown-with-msg? AssertionError #">= \(apply" (schedule/solve talks session-constraints))
       [["a" 1] ["b" 2]] [{:min 4 :max 4}]
       [["a" 1] ["b" 2]] [{:min 2 :max 2} {:min 2 :max 2}])])

  (testing "At least each session must have a talk"
    [(is (thrown-with-msg? AssertionError #">= \(count"
                           (schedule/solve [["a" 2]] [{:min 1 :max 1} {:min 1 :max 1}])))])

  (testing "A talk can fit in at least one session"
    [(are [talks session-constraints]
       (thrown-with-msg? AssertionError #"apply =" (schedule/solve talks session-constraints))
       [["a" 10] ["b" 20]] [{:min 15 :max 15} {:min 15 :max 15}]
       [["a" 10] ["b" 20] ["c" 30]] [{:min 15 :max 15} {:min 15 :max 15} {:min 30 :max 30}])]))