(defproject batch-address-test-tool "0.1.0-SNAPSHOT"
  :description "An address verification tool"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [turbovote.resource-config "0.2.1"]
                 [com.novemberain/langohr "4.0.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [amazonica "0.3.102"]
                 [com.climate/clj-newrelic "0.2.1"]
                 [org.clojure/data.csv "0.1.4"]
                 [clj-time "0.13.0"]
                 [clj-http "3.6.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-fuzzy "0.4.0"]]
  :plugins [[com.pupeno/jar-copier "0.4.0"]]
  :profiles {:dev {:resource-paths ["dev-resources"]
                   :source-paths ["dev-src/clj"]}}
  :java-agents [[com.newrelic.agent.java/newrelic-agent "3.25.0"]]
  :jar-copier {:java-agents true
               :destination "resources/jars"}
  :prep-tasks ["javac" "compile" "jar-copier"]
  :main vip.batch-address-test-tool.core)
