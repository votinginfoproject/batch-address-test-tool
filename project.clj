(defproject batch-address-test-tool "0.1.0-SNAPSHOT"
  :description "An address verification tool"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [turbovote.resource-config "0.2.0"]
                 [com.novemberain/langohr "3.5.1"]
                 [org.apache.logging.log4j/log4j-core "2.5"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.5"]
                 [org.slf4j/slf4j-api "1.7.20"]
                 [org.clojure/tools.logging "0.3.1"]]
  :profiles {:dev {:resource-paths ["dev-resources"]}}
  :main vip.batch-address-test-tool.core)
