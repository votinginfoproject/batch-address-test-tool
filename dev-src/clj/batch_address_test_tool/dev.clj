(ns batch-address-test-tool.dev
  (:require [vip.batch-address-test-tool.processor :as p])
  (:gen-class))

(defn test-parse-file [file-path]
  (let [ctx {:address-file-contents (slurp file-path)}]
    (try
      (-> ctx
          p/validate-and-parse-file
          clojure.pprint/pprint)
      (catch Exception ex
        (println "error!")
        (clojure.pprint/pprint ex)))))

(defn clean-line-endings [file-path]
  (let [in-file (slurp file-path)]
    (try
      (spit (str "corrected-" file-path)
            (clojure.string/replace in-file #"\r\n" "\n"))
      (catch Exception ex
        (println "error!")
        (clojure.pprint/pprint ex)))))

(defn -main [filename]
  (test-parse-file filename))
