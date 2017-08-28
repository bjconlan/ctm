(ns io.github.bjconlan.ctm.schedule-test
  (:require [clojure.test :refer :all]
            [io.github.bjconlan.ctm.schedule :as schedule]))

(let [fitness #'schedule/fitness]
  (fitness [["a" 1] ["b" 2] ["c" 3] ["d" 4]] {:min 10 :max 15})
  #_(fitness [["a" 1] ["b" 2] ["c" 3] ["d" 4]] {:min 10 :max 10})
  #_(fitness [["b" 2] ["c" 3] ["d" 4]] {:min 10 :max 10})
  #_(fitness [["a" 1] ["b" 2] ["c" 3] ["d" 4]] {:min 9 :max 9}))
