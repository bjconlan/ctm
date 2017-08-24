(ns io.github.bjconlan.ctm.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [io.github.bjconlan.ctm.input-format :as input-format]))

(defn- print-usage []
  (prn "Enter a file name (or any supported java path or even raw input string)"))

(defn main [args]
  (if-let [input-source (first args)]
    ;; TODO handle reader errors
    (with-open [buffered-reader (io/reader input-source)]
      (reduce (fn [results line]
                (try
                  (conj results (input-format/parse-line line))
                  (catch Exception e
                    (prn (str "Failed to parse \"" line \"". It will be omitted. " (.getMessage e)))
                    results))) [] (line-seq buffered-reader)))
    (print-usage)))

(main *command-line-args*)
(main ["test-input1.txt"])