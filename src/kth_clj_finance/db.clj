(ns kth-clj-finance.db
  (:require [amazonica.aws.dynamodbv2 :as dynamo]
            [clojure.string :as string]))

(def table-name "kth-clj-finance")

(defn tag-item [type item]
  (update item :id #(format "%s:%s" (string/upper-case (name type)) %)))

(defn untag-item [item]
  (update item :id string/replace-first #"[A-Z]+:" ""))

(defn get-account [id]
  (->> (dynamo/get-item :table-name table-name
                        :key {:id {:s id}})
       :item
       untag-item))

(defn list-accounts []
  (->> (dynamo/scan :table-name table-name)
       :items
       (filter #(string/starts-with? (:id %) "ACCOUNT:"))
       (map untag-item)))

(defn put-account [account]
  (dynamo/put-item :table-name table-name
                   :item (tag-item :account account)))

(defn put-transfer [transfer]
  (dynamo/put-item :table-name table-name
                   :item (tag-item :transfer transfer)))
