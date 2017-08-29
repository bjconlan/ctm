(ns io.github.bjconlan.ctm.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [io.github.bjconlan.ctm.input-format :as input-format]
            [io.github.bjconlan.ctm.schedule :as schedule])
  (:import (java.time LocalTime)
           (java.time.format DateTimeFormatter)))

;; this sessions vector represents the collection of sessions and their
;; particular constraints to the problem (in this case they need to fall
;; between a min/max number of minutes to be considered acceptable).
(def ^:private session-constraints [{:min 180 :max 180} {:min 180 :max 240}       ; Day 1
                                    {:min 180 :max 180} {:min 180 :max 240}])     ; Day 2

(defn- print-usage []
  (printf "Enter a file name (or any supported java url path)\n"))

(defn- print-schedule [scheduled-sessions]
  (let [time-format (DateTimeFormatter/ofPattern "hh:mma")]
    (map-indexed (fn [i [morning-session afternoon-session]]
                   (do (printf (str "Track " (inc i) ":\n"))
                       (reduce (fn [time [talk-title talk-minutes]]
                                 (do (printf (str (.format time time-format) " " talk-title "\n"))
                                     (.plusMinutes time talk-minutes)))
                               (LocalTime/of 9 0)
                               (concat (conj (:talks morning-session) ["Lunch" 60])
                                       (conj (:talks afternoon-session) ["Networking event" 60])))
                       (prn)))
                 (partition 2 (sort-by :index scheduled-sessions)))))

(defn main [args]
  (if-let [input-source (first args)]
    ;; TODO handle reader errors
    (-> (with-open [buffered-reader (io/reader input-source)]
          (reduce (fn [results line]
                    (try
                      (conj results (input-format/parse-line line))
                      (catch Exception e
                        (prn (str "Failed to parse \"" line \" "."
                                  "It will be omitted. " (.getMessage e)))
                        results)))
                  [] (line-seq buffered-reader)))
        (schedule/solve session-constraints)
        (print-schedule))
    (print-usage)))

(main ["sample.txt"])

#_(main *command-line-args*)