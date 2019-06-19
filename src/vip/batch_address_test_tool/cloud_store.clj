(ns vip.batch-address-test-tool.cloud-store
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [turbovote.resource-config :refer [config]])
  (:import [com.amazonaws HttpMethod]
           [com.amazonaws.auth BasicAWSCredentials
            AWSStaticCredentialsProvider]
           [com.amazonaws.regions Regions]
           [com.amazonaws.services.s3 AmazonS3Client
                                      AmazonS3ClientBuilder]
           [com.amazonaws.services.s3.model GeneratePresignedUrlRequest]))

(defn s3-client
  ([]
   (s3-client (config [:aws :creds :access-key])
              (config [:aws :creds :secret-key])
              (config [:aws :s3 :region])))
  ([access-key secret-key region]
   (aws/client
    {:api                  :s3
     :region               region
     :credentials-provider (credentials/basic-credentials-provider
                            {:access-key-id     access-key
                             :secret-access-key secret-key})})))

(defn fetch-file
  "Retrieves a given file from s3"
  ([bucket-name file-name]
   (fetch-file (s3-client) bucket-name file-name))
  ([s3-client bucket-name file-name]
   (log/info "fetching" file-name "from" bucket-name)
   (-> (aws/invoke s3-client
                   {:op :GetObject
                    :request {:Bucket bucket-name
                              :Key file-name
                              :Range 0}})
       :Body
       slurp)))


(defn save-file
  "Saves the upload file on s3 in the bucket with the give file name"
  ([bucket-name file-name upload-file]
   (save-file (s3-client) bucket-name file-name upload-file))
  ([s3-client bucket-name file-name upload-file]
   (aws/invoke s3-client
               {:op :PutObject
                :request {:Bucket bucket-name
                          :Key file-name
                          :Body (io/input-stream upload-file)}})))

(defn java-client
  ([]
   (java-client (config [:aws :creds :access-key])
                (config [:aws :creds :secret-key])
                (config [:aws :s3 :region])))
  ([access-key secret-key region]
   (let [java-region (-> region Regions/fromName)
         creds (BasicAWSCredentials. access-key secret-key)
         cred-provider (AWSStaticCredentialsProvider. creds)]
     (-> (AmazonS3ClientBuilder/standard)
         (.withCredentials cred-provider)
         (.withRegion java-region)
         .build))))

(defn url-for-file
  "Generates a pre-signed URL to access a given file on s3 with access for
   up to 3 days"
  ([bucket-name file-name]
   (url-for-file (java-client) bucket-name file-name))
  ([java-client bucket-name file-name]
   (let [expiration (-> 3 time/days time/from-now coerce/to-date)
         builder (-> (GeneratePresignedUrlRequest. bucket-name file-name)
                     (.withMethod HttpMethod/GET)
                     (.withExpiration expiration))]
     (.generatePresignedUrl java-client builder))))
