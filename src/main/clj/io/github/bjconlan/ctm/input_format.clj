(ns io.github.bjconlan.ctm.input-format
  "Functions for parsing input described in the 'Conference track management'
  problem. It's very naïve and has been designed to be concise to what is
  outlined by said documentation"
  (:require [clojure.string :as string])
  (:import (java.text ParseException)))

(defn- parse-duration
  "Takes a string of that fulfills '[012147483647]+min' or 'lightning' and
  transforms it into an integer number (where lightning is converted to 5)"
  [^String duration]
  (if (= "lightning" duration)
    5
    (-> (re-find #"^(\d+)min" duration)
        (second)
        (Integer/parseInt))))

(defn parse-line
  "Takes a string (line) from the input and attempts to transform it into
  title, duration pairs. Examples of the expected input:

    Common Ruby Errors 45min
    Rails for Python Developers lightning

  Where the duration is defined by '${1 - 2147483647}min' or 'lightning' text.
  If 'duration' section of the provided string is unable to be resolved then
  a ParseException is thrown.

  NOTE when only a duration is presented this is used to represent the title"
  [^String line]
  (if-let [[_ duration-str] (re-find #"(\d+min|lightning) *$" line)]
    [line (parse-duration duration-str)]
    (throw (ParseException. (str "Unable to locate duration identifier from " line) 0))))