(ns vip.batch-address-test-tool.processor
  (:require [clojure.tools.logging :as log]
            [clojure.data.csv :as csv]
            [clojure.string :as str]
            [vip.batch-address-test-tool.queue :as q]
            [vip.batch-address-test-tool.cloud-store :as cloud-store]
            [clojure.string :as str]
            [vip.batch-address-test-tool.civic-info :as civic-info]
            [vip.batch-address-test-tool.match :as match])
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
  (log/info "retrieve-file: " (pr-str ctx))
  (try
    (let [file-name   (get-in ctx [:input "fileName"])
          bucket-name (get-in ctx [:input "bucketName"])
          file        (cloud-store/fetch-file bucket-name file-name)]
      (assoc ctx :address-file-contents file))
    (catch Exception ex
      (log/error ex)
      (assoc ctx :error ex))))

(defn ->exception-with-line
  [msg line-num]
  (Exception. (str msg " on line number "(+ 1 line-num))))

(defn validate-header-row
  "Validates the header row of the input file to confirm 2 headers with
   expected names"
  [[line-num header-row]]
  (if (not= (count header-row) 2)
    (throw (->exception-with-line "Incorrect number of headers, expected 2"
                                  line-num))
    (if (not= (first header-row) "voter_address")
      (throw (->exception-with-line
              (str "Expected first header item to be \"voter_address\", was \""
                   (first header-row) "\"")
              line-num))
      (if (not= (second header-row) "expected_polling_location")
        (throw (->exception-with-line
                "Expected second header item to be \"expected_polling_location\""
                line-num))))))

(defn validate-and-parse-row
  "Validates number of elements in a row and returns row values parsed into
   a map"
  [[line-num row]]
  (if (not= (count row) 2)
    (throw (->exception-with-line "Incorrect number of row elements, expected 2"
                                  line-num))
    {:address (first row)
     :expected-polling-location (second row)}))

(defn not-blank-row? [[line-num row]]
  (not (and (= 1 (count row))
            (str/blank? (first row)))))

(defn cleanup-source-file*
  "Step for cleanup of source file contents, currently only
   replaces Windows weird line endings, but could add more
   steps in the future."
  [ctx]
  (try
    (let [original-csv (:address-file-contents ctx)
          fixed-csv (str/replace original-csv #"\r\n" "\n")]
      (assoc ctx :address-file-contents fixed-csv))
    (catch Exception ex
      (log/error ex)
      (assoc ctx :error ex))))

(def cleanup-source-file (->pass-through cleanup-source-file*))

(defn validate-and-parse-file*
  "Calls validate-header-row on first row in file and maps remaining rows with
   validate-and-parse-row"
  [ctx]
  (log/info "validate-and-parse-file: " (pr-str ctx))
  (try
    (let [csv (csv/read-csv (:address-file-contents ctx))
          indexed-rows (map-indexed (fn [idx rw] [idx rw])
                                    (take 301 csv))
          rows (filter not-blank-row? indexed-rows)]
      (validate-header-row (first rows))
      (assoc ctx :addresses
             (doall (map validate-and-parse-row (rest rows)))))
    (catch Exception ex
      (log/error ex)
      (assoc ctx :error ex))))

(def validate-and-parse-file (->pass-through validate-and-parse-file*))

(defn retrieve-polling-locations*
  [ctx]
  (log/info "retrieve-polling-locations: " (pr-str ctx))
  (let [addresses (:addresses ctx)
        polling-location-info (map #(civic-info/address->polling-location-info
                                     (:address %)) addresses)
        merged (map #(merge %1 %2) addresses polling-location-info)]
    (assoc ctx :addresses merged)))

(def retrieve-polling-locations (->pass-through retrieve-polling-locations*))

(defn calculate-scores*
  [ctx]
  (log/info "calculate-scores: " (pr-str ctx))
  (let [addresses (:addresses ctx)
        scores (map match/calculate-score addresses)
        merged (map #(assoc %1 :score %2) addresses scores)]
    (assoc ctx :addresses merged)))

(def calculate-scores (->pass-through calculate-scores*))

(defn calculate-matches*
  [ctx]
  (log/info "calculate-matches: " (pr-str ctx))
  (let [addresses (:addresses ctx)
        matches (map match/calculate-match addresses)
        merged (map #(assoc %1 :match %2) addresses matches)]
    (assoc ctx :addresses merged)))

(def calculate-matches (->pass-through calculate-matches*))

(defn ->result-row
  "Creates a vector of values from result suitable for csv writing"
  [result]
  (map #(get result % "") [:address :expected-polling-location :api-result :polling-location-count :match]))

(defn ->results-file
  "Creates the csv data and writes it to a temp file"
  [ctx]
  (let [header [["voter_address" "expected_polling_location" "api_result" "polling_location_count" "status"]]
        addresses (:addresses ctx)
        sorted (reverse (sort-by :score addresses))
        result-rows (map ->result-row sorted)
        csv-data (concat header result-rows)
        temp-file (File/createTempFile "address-results" ".tmp")]
    (with-open [writer (clojure.java.io/writer temp-file)]
      (csv/write-csv writer csv-data))
    temp-file))

(defn prepare-response*
  "Saves output file to s3 and generates data for response message"
  [ctx]
  (log/info "prepare-response: " (pr-str ctx))
  (let [fips-code (get-in ctx [:input "fipsCode"])
        bucket-name (get-in ctx [:input "bucketName"])
        output-file-name (str/join "/" [fips-code "output" "results.csv"])
        output-file (->results-file ctx)]
    (cloud-store/save-file bucket-name output-file-name output-file)
    (assoc ctx :results {:file-name output-file-name
                         :bucket-name bucket-name
                         :url (.toString (cloud-store/url-for-file bucket-name output-file-name))})))

(def prepare-response (->pass-through prepare-response*))

(defn respond
  "Publishes response message to output queue"
  [ctx]
  (log/info "respond:" (pr-str ctx))
  (if-let [error (:error ctx)]
    (do
      (log/error "Error processing batch addresses: " (pr-str error))
      (q/publish-failure {"status" "error"
                          "error" {"message" (.getMessage error)}
                          "fipsCode" (get-in ctx [:input "fipsCode"])
                          "transactionId" (get-in ctx [:input "transactionId"])}))
    (let [response-message {"fileName" (get-in ctx [:results :file-name])
                            "bucketName" (get-in ctx [:results :bucket-name])
                            "status" "ok"
                            "url" (get-in ctx [:results :url])
                            "fipsCode" (get-in ctx [:input "fipsCode"])
                            "transactionId" (get-in ctx [:input "transactionId"])}]
      (q/publish-success response-message))))

(defn process-message
  "Takes an incoming message, downloads the file, validates the contents,
   makes Civic Info API calls, scores responses, prepares the output file,
   and puts output message on queue"
  [message]
  (let [ctx {:input message}]
    (log/info "process-message: " (pr-str ctx))
    (-> ctx
        retrieve-file
        cleanup-source-file
        validate-and-parse-file
        retrieve-polling-locations
        calculate-scores
        calculate-matches
        prepare-response
        respond)))
