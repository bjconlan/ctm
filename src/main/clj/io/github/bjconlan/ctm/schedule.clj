(ns io.github.bjconlan.ctm.schedule
  "The schedule namespace provides functions relating to scheduling the problem
  using a 'local search' NP hard style solver using a 'Meta heuristic' system."
  (:require [io.github.bjconlan.ctm.bin-packing :as bin-packing]))

;; This concept should be abandoned the fitness function here is based off a
;; hill-climbing algo which doesn't correlate to this problem'
;(defn- fitness
;  "This function calculates the merit of the current session based on a simple
;  heuristic as to how close the current total is to the 'max' of the session's
;  constraint range. The goal is to have the fitness equal to 0 (perfect) or
;  greater than 0 (valid but sessions can still take talks)
;
;  Or looking at other scheduling problems you can think of less than or equal
;  to zero as a hard constraint while greater than zero is a soft constraint"
;  [session session-constraints]
;  (let [talk-duration (apply + (map second session))
;        {:keys [min max]} session-constraints]
;    (cond
;      (= talk-duration max) 0
;      (< talk-duration min) (- talk-duration min)
;      (> talk-duration max) (- max talk-duration)
;      :else (- max talk-duration))))                                              ; within valid range but less than max

(defn- valid? [session session-constraints]
  (let [talk-duration (apply + (map second session))
        {:keys [min max]} session-constraints]
    (or (< talk-duration min) (> talk-duration max))))

(defn- acceptable?
  "A convenience function to test against all sessions and their constraints
  using the above fitness function (ie. anything with 0 or above is acceptable"
  [sessions session-constraints]
  {:pre [(coll? sessions)
         (coll? session-constraints)
         (= (count sessions) (count session-constraints))]}
  (every? #(apply valid? %) (map vector sessions session-constraints)))

;(defn- can-fit?
;  [talk session]
;  (let [{:keys [min max capacity] :or {capacity 0}} session
;        new-capacity (+ talk capacity)]
;    (and (<= new-capacity max) (>= new-capacity min))))


;; FIXME I really should have researched this problem better as the local
;;       search concept really doesn't suit this problem domain as well as
;;       I thought it would. I apologies in advance (this function kinda
;;       keeps getting tempered into shape through cases.
(defn- find-better-schedule
  "This function needs a name change; but keeping with the idea of this being
  some form of 'local search' implementation will be kept.

  This function actually validates if the solution is acceptable and if it
  isn't introduces some (terrible) local search 'optimizations' (ie tries to
  fix the results until an acceptable result is found)."
  [sessions session-constraints]
  (
      ())))

(defn solve
  "The entry point for starting the 'meta heuristic' solver seeded from a
  deterministic best-fit-decreasing algorithm.

  The function takes in a vector of talk-name and minute tuples as talks and
  a vector of maps taking the 'min' and 'max' values of each as constraints
  for the length of the session."
  [talks session-constraints]
  {:pre [(<= (apply + (map second talks)) (apply + (map :max session-constraints)))             ; all talks meet all maximum session constraints
         (>= (apply + (map second talks)) (apply + (map :min session-constraints)))             ; all talks meet all minimum session constraints
         (>= (count (remove zero? (map second talks)))
             (count (remove zero? (map :min session-constraints))))                             ; at least 1 talk for each session (with positive min constraint)
         (apply = true (map (fn [[talk-mins max-session-mins]] (<= talk-mins max-session-mins)) ; check that at least 1 session will fit the largest talk
                            (map vector
                                 (take (count session-constraints)
                                       (sort-by < (map second talks)))
                                 (take (count session-constraints)
                                       (sort-by < (map :max session-constraints))))))]}
  (let [talk-minutes        (map second talks)
        max-session-minutes (map :max session-constraints)
        scheduled-sessions  (reduce (fn [r session-talk-idxs]
                                      (conj r (mapv #(get talks %) session-talk-idxs))) []
                                    (bin-packing/best-fit-decreasing talk-minutes max-session-minutes))
        (if (acceptable? scheduled-sessions session-constraints)
          scheduled-sessions
          (find-better-schedule scheduled-sessions session-constraints))]))
