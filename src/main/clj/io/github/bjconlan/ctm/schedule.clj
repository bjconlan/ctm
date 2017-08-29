(ns io.github.bjconlan.ctm.schedule
  "The schedule namespace provides functions relating to the scheduling problem
  using a 'global optimum' NP hard style solver using a 'Meta heuristic' system."
  (:require [io.github.bjconlan.ctm.bin-packing :as bin-packing]))

(defn- fitness
  "This function calculates the merit of the current session based on a simple
  squaring of the session current duration (common in bin packing annealing).

  NOTE the original weighting is skewed to keep over and under filling
       consistent while weighting minutes between the min and max as half
  WARNING the half idea isn't fully formed and has some obvious problems"
  [{:keys [talks constraints] :as session}]
  (let [session-duration (apply + (map second talks))
        {:keys [min max]} constraints
        weighting (cond
                    (and (= min max) (<= session-duration max)) session-duration
                    (> session-duration max) (+ min (- max session-duration))
                    :else (+ min (* (- session-duration min) 0.5)))]
    (reduce * (repeat 2 weighting))))

(defn- valid?
  "A convenience function to check that a session fulfills the constraints
  defined by its session-constraints"
  [session session-constraints]
  {:pre [(coll? session)
         (map? session-constraints)
         (contains? session-constraints :min)
         (contains? session-constraints :max)]}
  (let [talk-duration (apply + (map second session))
        {:keys [min max]} session-constraints]
    (or (< talk-duration min) (> talk-duration max))))

(defn- move-session-talk
  "A convenience function for moving one talk from a session to another

  NOTE the from-session & to-session arguments here are populated session
       maps (records which contain :talks"
  [from-session to-session talk-duration]
  (let [talk (first (filter #(= talk-duration (second %)) (:talks from-session)))]
    [(update-in from-session :talks (fn [talks]
                                      (loop [[talk & next-talks] talks
                                             prev-talks []]
                                        (if (= talk-duration (second talk))
                                          (concat prev-talks next-talks)
                                          (recur next-talks (conj prev-talks talk))))))
     (update-in to-session :talks (fn [talks] (conj talks talk)))]))

(defn- next-local-optimum
  "Performs a 'local search' to find the best move to make using the fitness function

  Returns a tuple of sessions which have a single talk moved from one to the other"
  [sessions]
  (let [root-session (first (filter #(not (:valid %)) sessions))]
    (->> (mapcat
           (fn [probed-session]
             (map #(move-session-talk probed-session root-session %)
                  (distinct (map second (:talks probed-session)))))
           (filter (not= root-session) sessions))
         (apply max-key #(+ (fitness %1) (fitness %2))))))


(defn- anneal
  "Anneal performs a 'simulated annealing' optimization to the problem.

  NOTE if no solution is found the function returns nil"
  ([sessions] (anneal sessions 4 200))
  ([sessions temperature-min temperature-max]
   {:pre [(vector? sessions)]}
   (loop [sessions    sessions
          temperature temperature-max]
     (if (every? true (map :valid sessions))
       sessions
       (when (< temperature-min temperature-max)
         (let [[from-session to-session] (next-local-optimum sessions)
               fitness-delta (- (+ (fitness from-session) (fitness to-session))
                                (+ (fitness (get sessions (:index from-session)))
                                   (fitness (get sessions (:index to-session)))))]
           (recur (if (or (pos? fitness-delta) (< (rand) (/ fitness-delta temperature)))
                    (assoc sessions (:index from-session) from-session (:index to-session) to-session)
                    sessions)
                  (dec temperature))))))))

(defn solve
  "The entry point for starting the 'meta heuristic' solver seeded from a
  deterministic best-fit-decreasing algorithm.

  The function takes in a vector of talk-title and duration tuples as talks and
  a vector of maps taking the 'min' and 'max' values of each as constraints for
  the length of the session."
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
                                    (bin-packing/best-fit-decreasing talk-minutes max-session-minutes))]
    (->> (map vector scheduled-sessions session-constraints)
         (map-indexed (fn [index [talks session-constraints]]
                        {:index       index
                         :talks       talks
                         :constraints session-constraints
                         :valid       (valid? talks session-constraints)}))
         (anneal))))
