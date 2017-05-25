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

(defn ->handler [handler-fn]
  (fn [ch metadata ^bytes payload]
    (try
      (let [message (-> (String. payload)
                        edn/read-string)]
       (log/debug "Received a message:" (pr-str message))
       (handler-fn message))
      (catch Exception ex
        (log/error "Error consuming message" (String. payload))
        (log/error (.getMessage ex))))))

(defn setup-consumer
  [ch handler-fn]
  (lcons/subscribe ch "batch-address.file.submit"
                 (->handler handler-fn)
                 {:auto-ack true}))

(defn print-handler
  [message]
  (log/debug "I'm handing a message!" (pr-str message)))

(defn initialize []
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
            (setup-consumer ch print-handler)
            (log/info "Setup-consumer called")
            ch)))

(defn publish
  "Publish a message to the qa-engine topic exchange on the given
  routing-key. The payload will be converted to EDN."
  [payload routing-key]
  (log/debug routing-key "-" (pr-str payload))
  (lb/publish @rabbit-channel
              (config [:rabbitmq :exchange])
              routing-key
              (pr-str payload)
              {:content-type "application/edn" :type "batch-address.event"}))
