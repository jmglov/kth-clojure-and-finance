(ns kth-clj-finance.apigw-handler
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [kth-clj-finance.accounts :as accounts]
            [kth-clj-finance.db :as db])
  (:import (java.time Instant)
           (java.util UUID))
  (:gen-class :name apigw-handler
              :main false
              :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))

(defn log
  ([msg]
   (println msg))
  ([msg data]
   (println (json/generate-string {:msg msg
                                   :data data}))))

(defn create-account [{:keys [body]}]
  (let [{:keys [account-holder]} body
        _ (log (str "Creating account for: " account-holder))
        account (accounts/create-account account-holder)]
    (db/put-account account)
    (log "Account created" account)
    account))

(defn list-accounts [_]
  {:accounts (db/list-accounts)})

(defn get-account [{:keys [pathParameters]}]
  (let [{:keys [id]} pathParameters]
    (log (str "Getting account id: " id))
    (db/get-account id)))

(defn create-transfer [{:keys [body pathParameters]}]
  (let [{:keys [credit-account amount]} body
        debit-account (:id pathParameters)
        _ (log (format "Transferring %d from account %s to account %s"
                       amount debit-account credit-account))
        transfer {:id (str (UUID/randomUUID))
                  :debit-account debit-account
                  :credit-account credit-account
                  :amount amount
                  :date (str (Instant/now))}]
    (db/put-transfer transfer)
    (log "Transfer created" transfer)
    transfer))

(def handlers
  {"/accounts"
   {"GET" list-accounts
    "POST" create-account}
   "/accounts/{id}"
   {"GET" get-account}
   "/accounts/{id}/transfer"
   {"POST" create-transfer}})

(defn parse-body [{:keys [body] :as event}]
  (if body
    (update event :body json/parse-string true)
    event))

(defn -handleRequest [_ in out ctx]
  (let [event (json/parse-stream (io/reader in) true)
        _ (log "Processing request" event)
        handler-fn (get-in handlers [(:resource event) (:httpMethod event)])
        response-body (-> event
                          parse-body
                          handler-fn 
                          json/generate-string)
        response-status (case (:httpMethod event)
                          "GET" 200
                          "POST" 201)
        response {:statusCode response-status
                  :body response-body}]
    (json/generate-stream response (io/writer out))))
