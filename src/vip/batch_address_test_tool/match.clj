(ns vip.batch-address-test-tool.match
  (:require [clj-fuzzy.metrics :as metrics]))

(defn calculate-score
  "Calcuates the Levenshtein distance between expected polling location
   and api results or -1 if api-result is nil."
  [{:keys [expected-polling-location api-result]}]
  (if api-result
    (metrics/levenshtein expected-polling-location api-result)
    -1))

(defn calculate-match
  "Calcuates a match based on the score."
  [{:keys [score]}]
    (cond
      (= -1 score) "No Result"
      (> 10 score) "Match"
      (< 20 score) "Mismatch"
      :else "Possible Mismatch"))
