(ns vip.batch-address-test-tool.processor
  (:require [clojure.tools.logging :as log]
            [clojure.data.csv :as csv]
            [vip.batch-address-test-tool.queue :as q]
            [vip.batch-address-test-tool.cloud-store :as cloud-store]
            [clojure.string :as str]
            [vip.batch-address-test-tool.civic-info :as civic-info])
  (:import [java.io File]))

(defn ->pass-through
  "Defines a function that will only call source-fn if there isn't an error
  in the context"
  [source-fn]
  (fn [ctx]
    (if (contains? ctx :error)
      ctx
      (source-fn ctx))))

(defn retrieve-file
  "Retrieves the file for processing from s3"
  [ctx]
  (try
    (let [file-name   (get-in ctx [:input "fileName"])
          bucket-name (get-in ctx [:input "bucketName"])
          file        (cloud-store/fetch-file bucket-name file-name)]
      (assoc ctx :address-file file))
    (catch Exception ex
      (assoc ctx :error ex))))

(defn validate-header-row
  "Validates the header row of the input file to confirm 2 headers with
   expected names"
  [header-row]
  (if (not= (count header-row) 2)
    (throw (Exception. "Incorrect number of headers, expected 2"))
    (if (not= (first header-row) "voter_address")
      (throw (Exception. "Expected first header item to be \"voter_address\""))
      (if (not= (second header-row) "expected_polling_location")
        (throw (Exception. "Expected second header item to be \"expected_polling_location\""))))))

(defn validate-and-parse-row
  "Validates number of elements in a row and returns row values parsed into
   a map"
  [row]
  (if (not= (count row) 2)
    (throw (Exception. "Incorrect number of row elements, expected 2"))
    {:address (first row)
     :expected-polling-location (second row)}))

(defn validate-and-parse-file*
  "Calls validate-header-row on first row in file and maps remaining rows with
   validate-and-parse-row"
  [ctx]
  (try
    (let [rows (take 301 (csv/read-csv (:address-file ctx)))]
      (validate-header-row (first rows))
      (assoc ctx :addresses
             (doall (map validate-and-parse-row (rest rows)))))
    (catch Exception ex
      (assoc ctx :error ex))))

(def validate-and-parse-file (->pass-through validate-and-parse-file*))

(defn retrieve-polling-locations*
  [ctx]
  (let [addresses (:addresses ctx)
        polling-locations (map #(civic-info/address->polling-location (:address %)) addresses)
        merged (mapv #(assoc %1 :api-result %2) addresses polling-locations)]
    (assoc ctx :addresses merged)))

(def retrieve-polling-locations (->pass-through retrieve-polling-locations*))

(defn calculate-match*
  [ctx]
  ctx)

(def calculate-match (->pass-through calculate-match*))

(defn ->group
  "Extracts group number from the file name"
  [file-name]
  (first (str/split file-name #"/")))

(defn ->result-row
  "Creates a vector of values from result suitable for csv writing"
  [result]
  (map #(get result % "") [:address :expected-polling-location :api-result :status]))

(defn ->results-file
  "Creates the csv data and writes it to a temp file"
  [ctx]
  (let [header [["voter_address" "expected_polling_location" "api_result" "status"]]
        result-rows (map ->result-row (:addresses ctx))
        csv-data (concat header result-rows)
        temp-file (File/createTempFile "address-results" ".tmp")]
    (with-open [writer (clojure.java.io/writer temp-file)]
      (csv/write-csv writer csv-data))
    temp-file))

(defn prepare-response*
  "Saves output file to s3 and generates data for response message"
  [ctx]
  (let [group (->group (get-in ctx [:input "fileName"]))
        bucket-name (get-in ctx [:input "bucketName"])
        output-file-name (str/join "/" [group "output" "results.csv"])
        output-file (->results-file ctx)]
    (cloud-store/save-file bucket-name output-file-name output-file)
    (assoc ctx :results {:file-name output-file-name
                         :bucket-name bucket-name
                         :url (.toString (cloud-store/url-for-file bucket-name output-file-name))})))

(def prepare-response (->pass-through prepare-response*))

(defn respond
  "Publishes response message to output queue"
  [ctx]
  (if (contains? ctx :error)
    (q/publish "batch-address.file.complete" {"status" "error"
                                              "error" {"message" (.getMessage (:error ctx))}})
    (let [response-message {"fileName" (get-in ctx [:results :file-name])
                            "bucketName" (get-in ctx [:results :bucket-name])
                            "status" "ok"
                            "url" (get-in ctx [:results :url])}]
      (q/publish "batch-address.file.complete" response-message))))

(defn process-message
  "Takes an incoming message, downloads the file, validates the contents,
   makes Civic Info API calls, scores responses, prepares the output file,
   and puts output message on queue"
  [message]
  (let [ctx {:input message}]
    (-> ctx
        retrieve-file
        validate-and-parse-file
        retrieve-polling-locations
        calculate-match
        prepare-response
        respond)))
