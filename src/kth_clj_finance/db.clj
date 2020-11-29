(ns kth-clj-finance.db
  (:require [amazonica.aws.dynamodbv2 :as dynamo]))

(def table-name "kth-clj-finance")

(defn get-account [id]
  (->> (dynamo/get-item :table-name table-name
                        :key {:id {:s id}})
       :item))

(defn list-accounts []
  (->> (dynamo/scan :table-name table-name)
       :items))

(defn put-account [account]
  (dynamo/put-item :table-name table-name
                   :item account))
