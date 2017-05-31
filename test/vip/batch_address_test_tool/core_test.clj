(ns vip.batch-address-test-tool.core-test
  (:require [vip.batch-address-test-tool.core :refer :all]
            [clojure.test :refer :all]))

(deftest validate-header-row-test
  (testing "Throws error with less than two elements in header row"
    (is (thrown-with-msg? Exception #"Incorrect number of headers, expected 2"
                          (validate-header-row nil)))
    (is (thrown-with-msg? Exception #"Incorrect number of headers, expected 2"
                          (validate-header-row [])))
    (is (thrown-with-msg? Exception #"Incorrect number of headers, expected 2"
                          (validate-header-row ["voter_address"]))))
  (testing "Throws error with more than two elements in header row"
    (is (thrown-with-msg? Exception #"Incorrect number of headers, expected 2"
                          (validate-header-row ["voter_address" "expected_polling_location" "Polling Location Result"]))))
  (testing "Test first header element"
    (is (thrown-with-msg? Exception #"Expected first header item to be \"voter_address\""
                          (validate-header-row ["Voter Address" "expected_polling_location"]))))
  (testing "Test second header element"
    (is (thrown-with-msg? Exception #"Expected second header item to be \"expected_polling_location\""
                          (validate-header-row ["voter_address" "Polling Location Result"]))))
  (testing "Good header row"
    (try
      (validate-header-row ["voter_address" "expected_polling_location"])
      (catch Exception ex
        (is (= 0 1) "Unexpected exception validating a good header row")))))

(deftest validate-and-parse-row-test
  (testing "Throws error with less than two elements in row"
    (is (thrown-with-msg? Exception #"Incorrect number of row elements, expected 2"
                          (validate-and-parse-row nil)))
    (is (thrown-with-msg? Exception #"Incorrect number of row elements, expected 2"
                          (validate-and-parse-row [])))
    (is (thrown-with-msg? Exception #"Incorrect number of row elements, expected 2"
                          (validate-and-parse-row ["123 Main Street, Springfield, IL 12345"]))))
  (testing "Throws an error with more than 2 elements in a row"
    (is (thrown-with-msg? Exception #"Incorrect number of row elements, expected 2"
                          (validate-and-parse-row ["123 Main Street, Springfield, IL, 12345" "Homer Elementary School, 234 Main Street, Springfield, IL, 12345" "other unexpected data"]))))
  (testing "Good address row"
    (try
      (let [results (validate-and-parse-row ["123 Main Street, Springfield, IL, 12345" "Homer Elementary School, 234 Main Street, Springfield, IL, 12345"])]
        (is (= (results {:address "123 Main Street, Springfield, IL, 12345"
                         :expected-polling-location "Homer Elementary School, 234 Main Street, Springfield, IL, 12345"}))))
      (catch Exception ex
        (is (= 0 1) "Unexpected exception validating a good address row")))))
