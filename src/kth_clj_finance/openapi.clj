(ns kth-clj-finance.openapi
  (:require [clojure.string :as string]
            [camel-snake-kebab.core :as csk]
            [spec-tools.json-schema :as json-schema]))

(defn ->schema-name [spec]
  (->> [namespace name]
       (map #(csk/->PascalCase (% spec)))
       string/join))

(defn ->ref [spec]
  {"$ref" (str "#/components/schemas/" (->schema-name spec))})

(defn ->json-content [spec]
  {:content
   {"application/json"
    {:schema (->ref spec)}}})

(defn ->parameter [parameter-spec]
  {:name (name parameter-spec)
   :in "path"
   :required true
   :schema (->ref parameter-spec)})

(defn ->response [method response-spec]
  (let [response-code (case method
                        :get "200"
                        :post "201")]
    {response-code (merge {:description "Response"}
                          (->json-content response-spec))}))

(defn ->request [request-schema]
  (->json-content request-schema))

(defn ->path
  ([kv]
   (->path nil kv))

  ([lambda-function-name [path methods]]
   [path (->> methods
              (map (fn [[method {:keys [summary parameter request response]}]]
                     [method
                      (merge {:summary summary}
                             {:responses (->response method response)}
                             (when parameter
                               {:parameters [(->parameter parameter)]})
                             (when request
                               {:requestBody (->request request)})
                             (when lambda-function-name
                               {:x-amazon-apigateway-integration
                                {:type "aws_proxy"
                                 :httpMethod "POST"
                                 :uri (format "arn:aws:apigateway:eu-west-1:lambda:path/2015-03-31/functions/arn:aws:lambda:eu-west-1:289341159200:function:%s/invocations"
                                              lambda-function-name)
                                 :credentials "arn:aws:iam::289341159200:role/kth-clj-finance-apigw"}}))]))
              (into {}))]))

(defn ->schemas [specs]
  {:schemas (->> specs
                 (map (fn [spec] [(->schema-name spec) (json-schema/transform spec)]))
                 (into {}))})
