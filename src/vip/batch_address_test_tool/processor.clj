(ns vip.batch-address-test-tool.processor
  (:require [clojure.tools.logging :as log]
            [clojure.data.csv :as csv]
            [vip.batch-address-test-tool.queue :as q]
            [vip.batch-address-test-tool.cloud-store :as cloud-store]
            [clojure.string :as str])
  (:import [java.io File]))

(defn retrieve-file
  [ctx]
  (try
    (let [file-name   (get-in ctx [:input "fileName"])
          bucket-name (get-in ctx [:input "bucketName"])
          file        (cloud-store/fetch-file bucket-name file-name)]
      (assoc ctx :address-file file))
    (catch Exception ex
      (assoc ctx :error ex))))

(defn validate-header-row
  [header-row]
  (if (not= (count header-row) 2)
    (throw (Exception. "Incorrect number of headers, expected 2"))
    (if (not= (first header-row) "voter_address")
      (throw (Exception. "Expected first header item to be \"voter_address\""))
      (if (not= (second header-row) "expected_polling_location")
        (throw (Exception. "Expected second header item to be \"expected_polling_location\""))))))

(defn validate-and-parse-row
  [row]
  (if (not= (count row) 2)
    (throw (Exception. "Incorrect number of row elements, expected 2"))
    {:address (first row)
     :expected-polling-location (second row)}))

(defn validate-and-parse-file
  [ctx]
  (if (contains? ctx :error)
    ctx
    (try
      (let [rows (csv/read-csv (:address-file ctx))]
        (validate-header-row (first rows))
        ; TODO iterate with index to stop at 300 rows
        (assoc ctx :addresses
               (doall (map validate-and-parse-row (rest rows)))))
      (catch Exception ex
        (assoc ctx :error ex)))))


(defn retrieve-polling-locations
  [ctx]
  ctx)

(defn calculate-match
  [ctx]
  ctx)

(defn ->group
  [file-name]
  (first (str/split file-name #"/")))

(defn ->result-row
  [result]
  (map #(get result % "") [:address :expected-polling-location :api-result :score]))

(defn ->results-file
  [ctx]
  (let [header [["voter_address" "expected_polling_location" "api_result" "status"]]
        result-rows (map ->result-row (:addresses ctx))
        csv-data (concat header result-rows)
        temp-file (File/createTempFile "address-results" ".tmp")]
    (with-open [writer (clojure.java.io/writer temp-file)]
      (csv/write-csv writer csv-data))
    temp-file))

(defn prepare-response
  [ctx]
  (if (contains? ctx :error)
    ctx
    (let [group (->group (get-in ctx [:input "fileName"]))
          bucket-name (get-in ctx [:input "bucketName"])
          output-file-name (str/join "/" [group "output" "results.csv"])
          output-file (->results-file ctx)]
      (cloud-store/save-file bucket-name output-file-name output-file)
      (assoc ctx :results {:file-name output-file-name
                           :bucket-name bucket-name
                           :url (.toString (cloud-store/url-for-file bucket-name output-file-name))}))))

(defn respond
  [ctx]
  (if (contains? ctx :error)
    (println "got an error" (.getMessage (:error ctx)))
    (let [response-message {"fileName" (get-in ctx [:results :file-name])
                            "bucketName" (get-in ctx [:results :bucket-name])
                            "status" "ok"
                            "url" (get-in ctx [:results :url])}]
      (q/publish "batch-address.file.complete" response-message))))

(defn handler [message]
  (let [ctx {:input message}]
    (-> ctx
        retrieve-file
        validate-and-parse-file
        retrieve-polling-locations
        calculate-match
        prepare-response
        respond)))