(defproject batch-address-test-tool "0.1.0-SNAPSHOT"
  :description "An address verification tool"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [turbovote.resource-config "0.2.1"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.clojure/data.json "1.0.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [com.amazonaws/aws-java-sdk-core "1.11.777"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.777"]
                 [com.cognitect.aws/api "0.8.456"]
                 [com.cognitect.aws/endpoints "1.1.11.774"]
                 [com.cognitect.aws/sqs "770.2.568.0"]
                 [com.cognitect.aws/s3 "784.2.593.0"]
                 [com.cognitect.aws/sns "773.2.578.0"]
                 [democracyworks/squishy "3.1.0"]
                 [org.clojure/data.csv "1.0.0"]
                 [clj-time "0.15.2"]
                 [clj-http "3.10.1"]
                 [cheshire "5.10.0"]
                 [clj-fuzzy "0.4.1"]]
  :profiles {:dev {:resource-paths ["dev-resources"]
                   :source-paths ["dev-src/clj"]}}
  :prep-tasks ["javac" "compile"]
  :main vip.batch-address-test-tool.core)
