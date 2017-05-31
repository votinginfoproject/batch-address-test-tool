(ns vip.batch-address-test-tool.cloud-store
  (:require [amazonica.aws.s3 :as s3]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

(def conf {:endpoint "us-east-1"})

(defn fetch-file
  "Retrieves a given file from s3"
  [bucket-name file-name]
  (-> (s3/get-object conf :bucket-name bucket-name :key file-name :range 0)
    :input-stream
    slurp))


(defn save-file
  "Saves the upload file on s3 in the bucket with the give file name"
  [bucket-name file-name upload-file]
  (s3/put-object conf
                 :bucket-name bucket-name
                 :key file-name
                 :file upload-file))

(defn url-for-file
  "Generates a pre-signed URL to access a given file on s3 with access for
   up to 3 days"
  [bucket-name file-name]
  (let [access-duration (-> 3 time/days time/from-now coerce/to-date)]
    (s3/generate-presigned-url conf bucket-name file-name access-duration)))
