(ns vip.batch-address-test-tool.queue
  (:require [clojure.tools.logging :as log]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [langohr.basic :as lb]
            [langohr.consumers :as lcons]
            [clojure.edn :as edn]
            [turbovote.resource-config :refer [config]]))

(def rabbit-connection (atom nil))
(def rabbit-channel (atom nil))

(defn ->handler
  "Creates a function that can take a rabbit message, parse it as edn, and
   send it to the handler function"
  [handler-fn]
  (fn [ch metadata ^bytes payload]
    (try
      (let [message (-> (String. payload)
                        edn/read-string)]
       (log/debug "Received a message:" (pr-str message))
       (handler-fn message))
      (catch Exception ex
        (log/error "Error consuming message" (String. payload))
        (log/error ex)))))

(defn setup-consumer
  "Sets up the handler function to process messages on the
   batch-address.file.submit channel"
  [ch handler-fn]
  (lcons/subscribe ch "batch-address.file.submit"
                 (->handler handler-fn)
                 {:auto-ack true}))

(defn initialize
  "Connects to rabbit and sets up handler function to consume messages"
  [handler-fn]
  (let [max-retries 5]
    (loop [attempt 1]
      (try
        (reset! rabbit-connection
                (rmq/connect (or (config [:rabbitmq :connection])
                                 {})))
        (log/info "RabbitMQ connected.")
        (catch Throwable t
          (log/warn "RabbitMQ not available:" (.getMessage t) "attempt:" attempt)))
      (when (nil? @rabbit-connection)
        (if (< attempt max-retries)
          (do (Thread/sleep (* attempt 1000))
              (recur (inc attempt)))
          (do (log/error "Connecting to RabbitMQ failed. Bailing.")
            (throw (ex-info "Connecting to RabbitMQ failed" {:attempts attempt})))))))
  (reset! rabbit-channel
          (let [ch (lch/open @rabbit-connection)]
            (le/topic ch (config [:rabbitmq :exchange]) {:durable false :auto-delete true})
            (log/info "RabbitMQ topic set.")
            (setup-consumer ch handler-fn)
            ch)))

(defn publish-to-queue
  "Sends the payload to the given queue, printing it as a EDN string"
  [payload queue-name]
  (log/debug (pr-str payload) "->" queue-name)
  (lb/publish @rabbit-channel
              ""
              queue-name
              (pr-str payload)
              {:content-type "application/edn"}))

(defn publish-event
  "Publish a message to the batch-address topic exchange on the given
  routing-key. The payload will be converted to EDN."
  [payload routing-key]
  (log/debug (pr-str payload) "->" routing-key)
  (lb/publish @rabbit-channel
              (config [:rabbitmq :exchange])
              routing-key
              (pr-str payload)
              {:content-type "application/edn" :type "batch-address.event"}))
