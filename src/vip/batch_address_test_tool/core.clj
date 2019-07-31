(ns vip.batch-address-test-tool.core
  (:require [clojure.tools.logging :as log]
            [vip.batch-address-test-tool.queue :as q]
            [vip.batch-address-test-tool.processor :as processor])
  (:gen-class))

(defn -main [& args]
  (let [id (java.util.UUID/randomUUID)]
    (log/info "VIP Batch Address Test Tool starting up. ID:" id)
    (let [consumer-id (q/start-consumer processor/process-message)]
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn []
                                   (q/stop-consumer consumer-id)
                                   (log/info "VIP Batch Address Test Tool shutting down...")))))))
