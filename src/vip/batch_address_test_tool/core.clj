(ns vip.batch-address-test-tool.core
  (:require [clojure.tools.logging :as log]
            [vip.batch-address-test-tool.queue :as q]))


(defn -main [& args]
  (let [id (java.util.UUID/randomUUID)]
    (log/info "VIP Batch Address Test Tool starting up. ID:" id)
    (q/initialize)
    (q/publish {:id id :event "starting"} "batch-address.status")
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (q/publish {:id id :event "stopping"} "batch-address.status")
                                 (log/info "VIP Batch Address Test Tool shutting down..."))))))
