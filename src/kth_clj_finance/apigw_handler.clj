(ns kth-clj-finance.apigw-handler
  (:require [cheshire.core :as json]
            [clojure.java.io :as io])
  (:gen-class :name apigw-handler
              :main false
              :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))

(defn list-accounts [_]
  {:accounts []})

(def handlers
  {"/accounts"
   {"GET" list-accounts}})

(defn -handleRequest [_ in out ctx]
  (let [event (json/parse-stream (io/reader in) true)
        _ (prn event)
        handler-fn (get-in handlers [(:resource event) (:httpMethod event)])
        response-body (-> (handler-fn event)
                          json/generate-string)
        response-status (case (:httpMethod event)
                          "GET" 200
                          "POST" 201)
        response {:statusCode response-status
                  :body response-body}]
    (json/generate-stream response (io/writer out))))
