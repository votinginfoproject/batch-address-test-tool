(ns vip.batch-address-test-tool.file-store
  (:require [amazonica.aws.s3 :as s3]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

; a function to take in a bucket name and file name, download that file, save it to a tmp directory
; a function to take in a temp file name and the original bucket/file name and put in a new bucket

(defn fetch-file
  [bucket-name file-name]
  (-> (s3/get-object cred :bucket-name bucket-name :key file-name :range 0)
    :input-stream
    slurp))


(defn save-file
  [bucket-name file-name upload-file]
  (s3/put-object cred
                 :bucket-name bucket-name
                 :key file-name
                 :file upload-file))

(defn url-for-file
  [bucket-name file-name]
  (s3/generate-presigned-url  cred bucket-name file-name (-> 3 time/days time/from-now coerce/to-date)))
