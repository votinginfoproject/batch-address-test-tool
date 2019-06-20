(ns vip.batch-address-test-tool.queue
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as credentials]
            [squishy.core :as squishy]
            [turbovote.resource-config :refer [config]])
  (:import [com.amazonaws.regions Region Regions]))

(defn string->edn [handler]
  (fn [message]
    (handler (edn/read-string (:body message)))))

(defn arn->queue-name
  "Because we share configuration with different SQS libraries, some libraries
   want the ARN and others (like squishy) want only the queue name. Rather than
   maintain multiple configuration values, this function takes an ARN format
   and returns just the queue name so squishy can be happy. The queue name is
   the last thing after a forward slash."
  [arn]
  (-> arn
      (str/split #"\/")
      last))

(defn start-consumer
  "Start an sqs consumer that pulls messages from the request-queue, converts
  them to EDN, and sends them to the handler function."
  ([handler]
   (start-consumer
    (config [:aws :creds :access-key])
    (config [:aws :creds :secret-key])
    (config [:aws :sqs :region])
    (arn->queue-name (config [:aws :sqs :address-test-request]))
    (arn->queue-name (config [:aws :sqs :address-test-request-failure]))
    handler))
  ([access-key secret-key region request-queue failure-queue handler]
   (let [java-region (-> region Regions/valueOf Region/getRegion)
         creds {:access-key access-key
                :access-secret secret-key
                :region java-region}
         edn-handler (string->edn handler)]
     (log/info "Starting consumer on " request-queue " with cred " (pr-str creds))
     (squishy/consume-messages creds request-queue failure-queue edn-handler))))

(defn stop-consumer [consumer-id]
  (squishy/stop-consumer consumer-id))

;; Much/most of this is copied from `data-processor` verbatim, and if we had
;; a better config story here, we could make it into a library of some kind

(defn sns-client
  ([]
   (sns-client (config [:aws :creds :access-key])
               (config [:aws :creds :secret-key])
               (config [:aws :sns :region])))
  ([access-key secret-key region]
   (aws/client
    {:api                  :sns
     :region               region
     :credentials-provider (credentials/basic-credentials-provider
                            {:access-key-id     access-key
                             :secret-access-key secret-key})})))

(defn- publish
  [sns-client topic payload]
  (aws/invoke sns-client {:op :Publish
                          :request {:TopicArn topic
                                    :Message (pr-str payload)}}))

(defn publish-success
  "Publish a successful feed processing message to the topic."
  ([payload]
   (publish-success (sns-client)
                    (config [:aws :sns :address-test-success-arn])
                    payload))
  ([sns-client topic payload]
   (log/info "publishing success to " topic " with payload " (pr-str payload))
   (publish sns-client topic payload)))

(defn publish-failure
  "Publish a failed feed processing message to the topic."
  ([payload]
   (publish-failure (sns-client)
                    (config [:aws :sns :address-failure-failure-arn])
                    payload))
  ([sns-client topic payload]
   (log/info "publishing failure to " topic " with payload " (pr-str payload))
   (publish sns-client topic payload)))

