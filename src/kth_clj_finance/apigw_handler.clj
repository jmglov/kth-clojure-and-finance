(ns kth-clj-finance.apigw-handler
  (:require [cheshire.core :as json]
            [clojure.java.io :as io])
  (:gen-class :name apigw-handler
              :main false
              :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))

(defn -handleRequest [_ in out ctx]
  (let [event (json/parse-stream (io/reader in) true)]
    (prn event)
    (json/generate-stream {:status "OK"} (io/writer out))))
