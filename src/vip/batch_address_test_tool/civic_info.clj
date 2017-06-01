(ns vip.batch-address-test-tool.civic-info
  (:require [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [turbovote.resource-config :refer [config]]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(defn response->polling-location
  [response]
  (when-let [address (get-in response [:body :pollingLocations 0 :address])]
    (str/trim (str (:locationName address) ", "
                   (:line1 address) ", "
                   (:city address) ", "
                   (:state address) " "
                   (:zip address)))))

(defn address->polling-location
  [address]
  (let [api-key (config [:civic-info-api-key])
        url "https://www.googleapis.com/civicinfo/v2/voterinfo"
        params {"address" address
                "key" api-key
                "electionId" 2000}
        opts {:query-params params :as :json :debug true}
        response (client/get url opts)]
      (response->polling-location response)))
