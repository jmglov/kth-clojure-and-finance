(ns kth-clj-finance.db
  (:require [amazonica.aws.dynamodbv2 :as dynamo]
            [clojure.string :as string]))

(def accounts-table "kth-clj-finance-accounts")

(def ledger-table "kth-clj-finance-ledger")

(defn get-account [id]
  (->> (dynamo/get-item :table-name accounts-table
                        :key {:id {:s id}})
       :item))

(defn list-accounts []
  (->> (dynamo/scan :table-name accounts-table)
       :items))

(defn put-account [account]
  (dynamo/put-item :table-name accounts-table
                   :item account))

(defn put-transfer [transfer]
  (dynamo/put-item :table-name ledger-table
                   :item transfer))
