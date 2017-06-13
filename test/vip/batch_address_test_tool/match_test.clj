(ns vip.batch-address-test-tool.match-test
  (:require [vip.batch-address-test-tool.match :refer :all]
            [clojure.test :refer :all]))

(deftest calculate-score-test
  (testing "Exact match"
    (is (= 0 (calculate-score {:expected-polling-location "foo"
                               :api-result "foo"}))))
  (testing "Off by 1"
    (is (= 1 (calculate-score {:expected-polling-location "foo1"
                               :api-result "foo"}))))
  (testing "No API result"
    (is (= -1 (calculate-score {:expected-polling-location "foo"})))))

(deftest calculate-match-test
  (testing "Match"
    (doseq [n (range 10)]
        (is (= "Match" (calculate-match {:score n})))))
  (testing "Possible Mismatch"
    (doseq [n (range 10 21)]
        (is (= "Possible Mismatch" (calculate-match {:score n})))))
  (testing "Mismatch"
    (is (= "Mismatch" (calculate-match {:score 21}))))
  (testing "No Result"
    (is (= "No Result" (calculate-match {:score -1})))))
