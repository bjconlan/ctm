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
  (println "Enter a file name (or any supported java url path)"))

(defn- print-schedule [scheduled-sessions]
  (if scheduled-sessions
    (let [time-format (DateTimeFormatter/ofPattern "hh:mma")]
      (map-indexed
        (fn [i [morning-session afternoon-session]]
          (do (println (str "Track " (inc i) ":"))
              (reduce (fn [time [talk-title talk-minutes]]
                        (do (println (str (.format time time-format) " " talk-title))
                            (.plusMinutes time talk-minutes)))
                      (LocalTime/of 9 0)
                      (concat (conj (:talks morning-session) ["Lunch" 60])
                              (conj (:talks afternoon-session) ["Networking event" 60])))
              (println)))
        (partition 2 (sort-by :index scheduled-sessions))))
    (println "Unfortunately scheduling these talks was unsuccessful")))

(defn main [args]
  (if-let [input-source (first args)]
    ; Could perhaps do a better job than showing raw IO or Parse exceptions to
    ; the user here but for the most part are informative enough.
    (-> (with-open [buffered-reader (io/reader input-source)]
          (reduce (fn [results line]
                    (try
                      (conj results (input-format/parse-line line))
                      (catch Exception e
                        (print (str "Failed to parse \"" line \" "."
                                    "It will be omitted. " (.getMessage e)))
                        results)))
                  [] (line-seq buffered-reader)))
        (schedule/solve session-constraints)
        (print-schedule))
    (print-usage)))