(ns vip.batch-address-test-tool.core)

(defn -main [& args]
  (let [id (java.util.UUID/randomUUID)]
    (log/info "VIP Batch Address Test Tool starting up. ID:" id)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (log/info "VIP Batch Address Test Tool shutting down..."))))))
