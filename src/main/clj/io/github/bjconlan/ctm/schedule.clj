(ns io.github.bjconlan.ctm.schedule
  "The schedule namespace provides functions relating to the scheduling problem
  using a 'global optimum' NP hard style solver using a 'Meta heuristic' system."
  (:require [io.github.bjconlan.ctm.bin-packing :as bin-packing]))

(defn- fitness
  "This function calculates the merit of the current session based on a simple
  squaring of the sessions current duration (common in bin packing annealing).

  The basic algorithm is to take the overflow from underflow, square the result
  and the value caught between the min/max constraints simply add ie:
    duration = 200, constraints 100-150: (100 - 50)^2 + 50 = 2500 + 50 = 2550
    duration = 200, constraints 300-350: (200 - 0)^2 + 0 = 40000
    duration = 200. constraints 150-200: (150 - 0)^2 + 50 = 22550
    duration = 200. constraints 200-200: (200 - 0)^2 + 0 = 40000
    duration = 200, constraints 50-100: (0)^2 + 50 = 50 (special case)

  NOTE When the overflow is greater than the min constraint we use 0."
  [{:keys [talks constraints] :as session}]
  (let [session-duration (apply + (map second talks))
        {:keys [min max]} constraints
        before-min       (clojure.core/min min session-duration)
        after-max        (clojure.core/max (- session-duration max) 0)
        between          (- session-duration (+ before-min after-max))
        over-under-delta (- before-min after-max)]
    (+ (if (pos? over-under-delta)
         (reduce * (repeat 2 over-under-delta))
         0)
       between)))

(defn- valid?
  "A convenience function to check that a session fulfills the constraints
  defined by it's session-constraints"
  [{:keys [talks constraints] :as session}]
  (let [session-duration (apply + (map second talks))
        {:keys [min max]} constraints]
    (and (>= session-duration min) (<= session-duration max))))

(defn- move-session-talk
  "A convenience function for moving one talk from a session to another which
  has the duration of the specified talk-duration.

  Returns the updated from-session and to-sessions with the moved talk.

  NOTE If the talk duration cannot be located in the from-session the sessions
       are returned unchanged"
  [from-session to-session talk-duration]
  {:pre [(seq (:talks from-session))
         (vector? (:talks to-session))]}
  (if-let [talk (first (filter #(= talk-duration (second %)) (:talks from-session)))]
    [(update from-session
             :talks (fn [talks]
                      (vec (loop [[talk & next-talks] talks
                                  prev-talks []]
                             (if (= talk-duration (second talk))
                               (concat prev-talks next-talks)
                               (recur next-talks (conj prev-talks talk)))))))
     (update to-session :talks (fn [talks] (conj talks talk)))]
    [from-session to-session]))

(defn- local-search-by-fitness
  "Performs a 'local search' to find the best move to make using the fitness
  function. This is done basically by finding a 'session' at random that fails
  the 'valid?' test. This session is then used to test against other session's
  talks (making this random helps the diversity of the algorithm).

  The sessions passed must contain at least 1 'invalid' session and contain at
  least 2 sessions (as to allow for a talk to be moved)

  Returns a tuple of sessions have been updated with a single talk moved from
  one to the other ['from session' 'to session']

  REVIEW This could become more generic (and idiomatic to the function name) by
         passing in the fitness function to this as an argument."
  [sessions]
  {:pre [(some #(not (valid? %)) sessions)
         (< 1 (count sessions))]}
  (let [root-session (rand-nth (filter #(not (valid? %)) sessions))]
    (->> (mapcat (fn [probed-session]
                   (map #(move-session-talk probed-session root-session %)
                        (distinct (map second (:talks probed-session)))))
                 (filter #(not= root-session %) sessions))
         (apply max-key #(+ (fitness (first %)) (fitness (second %)))))))

(defn- anneal
  "Anneal performs a 'simulated annealing' optimization to the problem. It
  takes the sessions and tests if it passes the validation test. On failure it
  perform an 'annealing' step again by moving 1 talk and re-evaluating the
  schedule (sessions). Annealing also introduces some randomness allowing worse
  choices to be made (these come from the temperature function vs the fitness
  where if the fitness difference

  Returns an optimized schedule (collection of sessions) in index order or nil
  if no solution is found. (or the schedule is empty)"
  [schedule temperature-min temperature-max]
  {:pre [(<= temperature-min temperature-max)]}
  (when (seq schedule)
    (if (and (= 1 (count schedule)) (not (valid? (first schedule)))) ; Handle edge case of single invalid session
      nil
      (loop [sessions    schedule
             temperature temperature-max]
        (if (every? true? (map valid? sessions))
          sessions
          (when (> temperature temperature-min)
            (let [[from-session to-session] (local-search-by-fitness sessions)
                  fitness-delta (- (+ (fitness from-session) (fitness to-session))
                                   (+ (fitness (nth sessions (:index from-session)))
                                      (fitness (nth sessions (:index to-session)))))]
              (recur (if (or (pos? fitness-delta)
                             (> (rand) (/ fitness-delta temperature)))
                       (assoc sessions (:index from-session) from-session
                                       (:index to-session) to-session)
                       sessions)
                     (dec temperature)))))))))

(defn solve
  "The entry point for starting the 'meta heuristic' solver seeded from a
  deterministic best-fit-decreasing algorithm.

  The function takes in a vector of talk-title and duration tuples as talks and
  a vector of maps taking the 'min' and 'max' values of each as constraints for
  the length of the session.

  If no solution is found nil is returned otherwise a collection of 'sessions'
  are returned (in index order)

  NOTE 'assert' is explicitly used here along with 'pre' constraints as human
       readable messages should be returned along with the assertion error"
  [talks session-constraints]
  {:pre [(every? true? (map (fn [{:keys [min max]}]
                              (and (<= min max) (<= 0 min) (<= 0 max)))
                            session-constraints))]}
  (do
    (assert (<= (apply + (map second talks))
                (apply + (map :max session-constraints))) "All talk durations must be less than schedule duration")
    (assert (>= (apply + (map second talks))
                (apply + (map :min session-constraints))) "All talk durations must meet the minimum schedule duration")
    (assert (>= (count (filter #(pos? (second %)) talks))
                (count (filter #(pos? (:min %)) session-constraints))) "At least one talk for each session must be specified")
    (assert (apply = true (map (fn [[talk-mins max-session-mins]]
                                 (<= talk-mins max-session-mins))
                               (map vector
                                    (take (count session-constraints)
                                          (sort > (map second talks)))
                                    (take (count session-constraints)
                                          (sort > (map :max session-constraints)))))) "Each talk must fit within a session")
    (let [talk-minutes        (map second talks)
          max-session-minutes (map :max session-constraints)
          scheduled-sessions  (reduce (fn [r session-talk-idxs]
                                        (conj r (mapv #(get talks %) session-talk-idxs)))
                                      [] (bin-packing/best-fit-decreasing
                                           talk-minutes max-session-minutes))]
      (anneal (->> (map vector scheduled-sessions session-constraints)
                   (map-indexed (fn [index [talks session-constraints]]
                                  {:index       index
                                   :talks       talks
                                   :constraints session-constraints}))
                   (vec))
              ; The numbers here are the min & max annealing denominators (used
              ; for a lowering random threshold) I've used 25 as the lowest
              ; because a 'lightning talk' is 5 minutes and the fitness function
              ; will square durations (below the session minimum). 2000 is just a
              ; random approximation based off a ~6% of the squared min session
              ; constraint for all session (180^2). But this really is arbitrary
              25 2000))))
