(ns io.github.bjconlan.ctm.branch-and-bound)

(def talks [60 45 30 45 45 5 60 45 30 30 45 60 60 45 30 30 60 30 30])
(def sessions [{:min 180 :max 180} {:min 180 :max 260}
               {:min 180 :max 180} {:min 180 :max 260}])

(defn- can-fit? [talk session]
  (let [{:keys [min max capacity] :or {capacity 0}} session
        new-capacity (+ talk capacity)]
    (and (<= new-capacity max) (>= new-capacity min))))



;; Constraints (just functions that build up predicate functions
;; (predicate functions simply take a single arg and return a boolean value)
(defn equal-to? [x] (partial = x))
(defn less-than? [x] (partial > x))
(defn greater-than? [x] (partial < x))
(defn between? [min max] #(and ((greater-than? min) %) ((less-than? max) %)))