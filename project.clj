(defproject batch-address-test-tool "0.1.0-SNAPSHOT"
  :description "An address verification tool"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [turbovote.resource-config "0.2.1"]
                 [org.clojure/tools.logging "0.5.0-alpha.1"]
                 [org.clojure/data.json "0.2.6"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [com.amazonaws/aws-java-sdk-core "1.11.571"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.571"]
                 [com.cognitect.aws/api "0.8.305"]
                 [com.cognitect.aws/endpoints "1.1.11.565"]
                 [com.cognitect.aws/sqs "697.2.391.0"]
                 [com.cognitect.aws/s3 "722.2.468.0"]
                 [com.cognitect.aws/sns "718.2.444.0"]
                 [democracyworks/squishy "3.1.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [clj-time "0.15.1"]
                 [clj-http "3.10.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-fuzzy "0.4.1"]]
  :profiles {:dev {:resource-paths ["dev-resources"]
                   :source-paths ["dev-src/clj"]}}
  :prep-tasks ["javac" "compile"]
  :main vip.batch-address-test-tool.core)
