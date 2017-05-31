(ns vip.batch-address-test-tool.file-store
  (:require [amazonica.aws.s3 :as s3]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

(def conf {:endpoint "us-east-1"})

(defn fetch-file
  [bucket-name file-name]
  (-> (s3/get-object conf :bucket-name bucket-name :key file-name :range 0)
    :input-stream
    slurp))


(defn save-file
  [bucket-name file-name upload-file]
  (s3/put-object conf
                 :bucket-name bucket-name
                 :key file-name
                 :file upload-file))

(defn url-for-file
  [bucket-name file-name]
  (s3/generate-presigned-url conf bucket-name file-name (-> 3 time/days time/from-now coerce/to-date)))
