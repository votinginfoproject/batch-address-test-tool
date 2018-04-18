(ns vip.batch-address-test-tool.processor-test
  (:require [vip.batch-address-test-tool.processor :refer :all]
            [clojure.test :refer :all]))

(deftest validate-header-row-test
  (testing "Throws error with less than two elements in header row"
    (is (thrown-with-msg? Exception #"Incorrect number of headers, expected 2 on line number 1"
                          (validate-header-row [0 []])))
    (is (thrown-with-msg? Exception #"Incorrect number of headers, expected 2 on line number 1"
                          (validate-header-row [0 ["voter_address"]]))))
  (testing "Throws error with more than two elements in header row"
    (is (thrown-with-msg? Exception #"Incorrect number of headers, expected 2 on line number 2"
                          (validate-header-row [1 ["voter_address" "expected_polling_location" "Polling Location Result"]]))))
  (testing "Test first header element"
    (is (thrown-with-msg? Exception #"Expected first header item to be \"voter_address\""
                          (validate-header-row [1 ["Voter Address" "expected_polling_location"]]))))
  (testing "Test second header element"
    (is (thrown-with-msg? Exception #"Expected second header item to be \"expected_polling_location\""
                          (validate-header-row [0 ["voter_address" "Polling Location Result"]]))))
  (testing "Good header row"
    (try
      (validate-header-row [0 ["voter_address" "expected_polling_location"]])
      (catch Exception ex
        (is (= 0 1) "Unexpected exception validating a good header row")))))

(deftest validate-and-parse-row-test
  (testing "Throws error with less than two elements in row"
    (is (thrown-with-msg? Exception #"Incorrect number of row elements, expected 2 on line number 2"
                          (validate-and-parse-row [1 []])))
    (is (thrown-with-msg? Exception #"Incorrect number of row elements, expected 2 on line number 3"
                          (validate-and-parse-row [2 ["123 Main Street, Springfield, IL 12345"]]))))
  (testing "Throws an error with more than 2 elements in a row"
    (is (thrown-with-msg? Exception #"Incorrect number of row elements, expected 2 on line number 3"
                          (validate-and-parse-row [2 ["123 Main Street, Springfield, IL, 12345" "Homer Elementary School, 234 Main Street, Springfield, IL, 12345" "other unexpected data"]]))))
  (testing "Good address row"
    (try
      (let [results (validate-and-parse-row [2 ["123 Main Street, Springfield, IL, 12345" "Homer Elementary School, 234 Main Street, Springfield, IL, 12345"]])]
        (is (= (results {:address "123 Main Street, Springfield, IL, 12345"
                         :expected-polling-location "Homer Elementary School, 234 Main Street, Springfield, IL, 12345"}))))
      (catch Exception ex
        (is (= 0 1) "Unexpected exception validating a good address row")))))

(deftest cleanup-source-file*-test
  (testing "leaves a good file content alone"
    (let [ctx {:address-file-contents "voter_address,expected_polling_location\n1,2\n"}
          cleaned-ctx (cleanup-source-file* ctx)]
      (is (= "voter_address,expected_polling_location\n1,2\n"
             (:address-file-contents cleaned-ctx)))))
  (testing "cleans up pesky windows line endings"
    (let [ctx {:address-file-contents "voter_address,expected_polling_location\r\n1,2\r\n"}
          cleaned-ctx (cleanup-source-file* ctx)]
      (is (= "voter_address,expected_polling_location\n1,2\n"
             (:address-file-contents cleaned-ctx))))))

(deftest validate-and-parse-file*-test
  (testing "skips empty rows, even at top of file"
    (let [ctx {:address-file-contents "\nvoter_address,expected_polling_location\n\n1,2\n\n"}
          parsed-ctx (validate-and-parse-file* ctx)]
      (is (= 1 (count (:addresses parsed-ctx))))
      (is (= {:address "1"
              :expected-polling-location "2"}
             (-> parsed-ctx :addresses first))))))

(deftest ->result-row-test
  (testing "Partial result"
    (is (= ["foo" "bar" "" "" ""] (->result-row {:address "foo" :expected-polling-location "bar"}))))
  (testing "Full result"
    (is (= ["foo" "bar" "baz" 1 "blee"] (->result-row {:address "foo"
                                                       :expected-polling-location "bar"
                                                       :api-result "baz"
                                                       :polling-location-count 1
                                                       :match "blee"})))))
