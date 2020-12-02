(ns kth-clj-finance.clouds
  (:require [kth-clj-finance.accounts :as accounts]
            [amazonica.aws.dynamodbv2 :as dynamo]))

;; 21st century banking, please!
(comment

















  ;; Let's create an API:
  ;; POST /accounts                ; Create an account
  ;; GET  /accounts                ; List accounts
  ;; GET  /accounts/{id}           ; Get account info
  ;; POST /accounts/{id}/transfer  ; Transfer from this account to another
















  ;; Say it with Clojure

  (def paths
    {"/accounts"
     {:post {:summary "Create an account"}
      :get {:summary "List accounts"}}

     "/accounts/{id}"
     {:get {:summary "Get account info"}}

     "/accounts/{id}/transfer"
     {:post {:summary "Transfer from this account to another"}}})


















  ;; What do we need for an account?
















  (require '[kth-clj-finance.accounts :as accounts])
















  (accounts/create-account "Ashley")
















  ;; Say it with spec!

  (require '[clojure.spec.alpha :as s])
















  (s/def :account/id (s/and string?
                            (partial re-matches #"[0-9]{7}")))
















  (s/valid? :account/id (accounts/new-account-number))

















  (s/def :account/account-holder string?)

















  (s/def :account/date-opened (s/and string?
                                     (partial re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z")))















  (import '(java.time Instant))
















  (s/valid? :account/date-opened (str (Instant/now)))
















  (s/def :account/account (s/keys :req-un [:account/id
                                           :account/account-holder
                                           :account/date-opened]))















  (s/valid? :account/account (accounts/create-account "Ashley"))
















  ;; When creating an account, we only need holder's name

  (s/def :account/create-request
    (s/keys :req-un [:account/account-holder]))

















  (def paths
    {"/accounts"
     {:post {:summary "Create an account"
             :request :account/create-request
             :response :account/account}
      :get {:summary "List accounts"
            :response :???}}

     "/accounts/{id}"
     {:get {:summary "Get account info"
            :response :account/account}}

     "/accounts/{id}/transfer"
     {:post {:summary "Transfer from this account to another"
             :request :???
             :response :???}}})
















  ;; What should list accounts return?
















  (s/def :account/accounts (s/coll-of :account/account))

















  (s/def :accounts/list (s/keys :req-un [:account/accounts]))

















  (def paths
    {"/accounts"
     {:post {:summary "Create an account"
             :request :account/create-request
             :response :account/account}
      :get {:summary "List accounts"
            :response :account/accounts}}

     "/accounts/{id}"
     {:get {:summary "Get account info"
            :response :account/account}}

     "/accounts/{id}/transfer"
     {:post {:summary "Transfer from this account to another"
             :request :???
             :response :???}}})


















  ;; How do I create a transfer?

















  (s/def :transfer/id string?)

















  (s/def :transfer/debit-account :account/id)

















  (s/def :transfer/credit-account :account/id)

















  (s/def :transfer/amount pos-int?)
















  (s/def :transfer/date (s/and string?
                               (partial re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z")))
















  (s/def :transfer/transfer (s/keys :req-un [:transfer/id
                                             :transfer/debit-account
                                             :transfer/credit-account
                                             :transfer/amount
                                             :transfer/date]))
















  (s/def :transfer/request (s/keys :req-un [:transfer/credit-account
                                            :transfer/amount]))


















  (def paths
    {"/accounts"
     {:post {:summary "Create an account"
             :request :account/create-request
             :response :account/account}
      :get {:summary "List accounts"
            :response :accounts/list}}

     "/accounts/{id}"
     {:get {:summary "Get account info"
            :parameter :account/id
            :response :account/account}}

     "/accounts/{id}/transfer"
     {:post {:summary "Transfer from this account to another"
             :parameter :account/id
             :request :transfer/request
             :response :transfer/transfer}}})


















  ;; Now that we have sketched our API,
  ;; let's create it!

















  (require '[amazonica.core :as amazonica])

















  (defn refresh-aws-credentials [profile]
    (amazonica/defcredential (amazonica/get-credentials {:profile profile})))
















  (refresh-aws-credentials "jmglov")
















  (require '[amazonica.aws.apigateway :as apigw])

















  (apigw/get-rest-apis)


















  ;; OpenAPI 3 allows us to describe an API with data!
  ;;
  ;; https://swagger.io/docs/specification/basic-structure/

  (require '[kth-clj-finance.openapi :as openapi])

















  ;; We can turn our specs in JSON schemas!
















  (->> (s/registry)
       keys
       shuffle
       (take 10))
















  (require '[clojure.string :as string])

















  (defn user-specs []
    (->> (s/registry)
         keys
         (remove #(string/starts-with? (namespace %) "clojure"))))

















  (user-specs)

















  ;; Oops, we still have a problem

















  (defn user-specs []
    (->> (s/registry)
         keys
         (filter keyword?)
         (remove #(string/starts-with? (namespace %) "clojure"))))

















  (user-specs)

















  ;; Now we can create an OpenAPI spec
















  (def api
    {:openapi "3.0.0"
     :info {:title "KTH Bank API"
            :version "1.0"}
     :paths (->> paths
                 (map openapi/->path)
                 (into {}))
     :components (openapi/->schemas (user-specs))})

















  api


















  ;; To turn this into an API,
  ;; we're gonna need some JSON

















  (require '[cheshire.core :as json])


















  (apigw/import-rest-api :endpoint-configuration-types "REGIONAL"
                         :body (json/generate-string api))



















  ;; https://eu-west-1.console.aws.amazon.com/apigateway/main/apis?region=eu-west-1

















  ;; OK, but where do we put the code?

















  
  ;; Handle the requests with a lamda function!

















  ;; To create a lambda function, build
  ;; the uberjar as per the README:
  ;;
  ;; make clean apigw

















  (require '[amazonica.aws.lambda :as lambda])


















  (lambda/list-functions)

















  ;; We need somewhere to put our uberjar
  ;; so that we can tell Lambda where to
  ;; find the code

















  (require '[amazonica.aws.s3 :as s3])
















  (s3/list-buckets)


















  (s3/put-object :bucket-name "misc.jmglov.net"
                 :key "kth-clj-finance/kth-clj-finance.jar"
                 :file "target/kth-clj-finance.jar")

















  (s3/list-objects-v2 :bucket-name "misc.jmglov.net"
                      :prefix "kth-clj-finance/")















  (lambda/create-function :function-name "kth-clj-finance-apigw-handler"
                          :handler "apigw-handler"
                          :runtime "java11"
                          :memory-size 1024
                          :role "arn:aws:iam::289341159200:role/kth-clj-finance-apigw-handler"
                          :code {:s3-bucket "misc.jmglov.net"
                                 :s3-key "kth-clj-finance/kth-clj-finance.jar"})


















  (def apigw-handler-lambda (lambda/get-function :function-name "kth-clj-finance-apigw-handler"))


















  (def apigw-handler-function (get-in apigw-handler-lambda [:configuration :function-name]))



















  (lambda/invoke :function-name apigw-handler-function
                 :payload (slurp "resources/apigw-test-request.json"))


















  (require '[amazonica.aws.logs :as logs])



















  (logs/describe-log-groups)
















  (logs/describe-log-streams :log-group-name "/aws/lambda/kth-clj-finance-apigw-handler")



















  (logs/get-log-events :log-group-name "/aws/lambda/kth-clj-finance-apigw-handler"
                       :log-stream-name
                       (->> (logs/describe-log-streams :log-group-name "/aws/lambda/kth-clj-finance-apigw-handler")
                            :log-streams
                            last
                            :log-stream-name))

















  (defn get-log-events [function-name]
    (let [log-group-name (str "/aws/lambda/" function-name)]
      (logs/get-log-events :log-group-name log-group-name
                           :log-stream-name
                           (->> (logs/describe-log-streams :log-group-name log-group-name)
                                :log-streams
                                last
                                :log-stream-name))))



















  ;; Now, we need the lambda to handle
  ;; API requests

















  (def api
    {:openapi "3.0.0"
     :info {:title "KTH Bank API"
            :version "1.0"}
     :paths (->> paths
                 (map (partial openapi/->path apigw-handler-function))
                 (into {}))
     :components (openapi/->schemas (user-specs))})



















  api















  (apigw/put-rest-api :rest-api-id api-id
                      :body (json/generate-string api))




















  ;; Now that our API has code behind it,
  ;; we can test it...

















  ;; ...almost. First, we need to deploy it.
  





















  (def api-id (->> (apigw/get-rest-apis)
                   :items
                   first
                   :id))
































  (apigw/create-deployment :rest-api-id api-id)



















  (require '[clj-http.client :as http])


















  (def base-url (format "https://%s.execute-api.eu-west-1.amazonaws.com/api"
                        api-id))


















  (http/get (str base-url "/accounts"))



















  (get-log-events apigw-handler-function)



















  ;; We need to fix our handler!


















  ;; Now to redeploy the lambda function

















  (require '[clojure.java.shell :refer [sh]])

















  (sh "make" "clean" "apigw")



















  (def apigw-handler-code {:function-name "kth-clj-finance-apigw-handler"
                           :s3-bucket "misc.jmglov.net"
                           :s3-key "kth-clj-finance/kth-clj-finance.jar"})


















  (s3/put-object :bucket-name (:s3-bucket apigw-handler-code)
                 :key (:s3-key apigw-handler-code)
                 :file "target/kth-clj-finance.jar")


















  (lambda/update-function-code apigw-handler-code)






































  (http/get (str base-url "/accounts"))


















  (-> (http/get (str base-url "/accounts"))
      :body
      (json/parse-string true))



















  ;; Now let's create some accounts!


















  (require '[clojure.spec.gen.alpha :as sgen])


















  (sgen/generate (s/gen :account/create-request))


















  (http/post (str base-url "/accounts")
             {:content-type :json
              :body (-> (sgen/generate (s/gen :account/create-request))
                        json/generate-string)})

















  ;; Oops, better implement create account!


















  ;; Redeploying the lambda is... ugh


















  (defn deploy-lambda []
    (sh "make" "clean" "apigw")
    (s3/put-object :bucket-name (:s3-bucket apigw-handler-code)
                 :key (:s3-key apigw-handler-code)
                 :file "target/kth-clj-finance.jar")
    (lambda/update-function-code apigw-handler-code))


















  (defn cheat [git-commit]
    (sh "git" "checkout" git-commit "--"
        "src/kth_clj_finance/apigw_handler.clj"))
















  (cheat "692397faed373940d0cae7ce434ece2b03bd39e4")

















  (deploy-lambda)

















  (-> (http/post (str base-url "/accounts")
                 {:content-type :json
                  :body (-> (sgen/generate (s/gen :account/create-request))
                            json/generate-string)})
      :body
      (json/parse-string true))


















  (-> (http/get (str base-url "/accounts/1314693"))
      :body
      (json/parse-string true))

















  (cheat "1e57895ca438974c09910aa1c188cbb9a29cf2ad")

















  (deploy-lambda)
















  (-> (http/get (str base-url "/accounts/1314693"))
      :body
      (json/parse-string true))

















  ;; Where can we put our data? Is there something
  ;; like an atom in the cloud?

















  (require '[amazonica.aws.dynamodbv2 :as dynamo])

















  (dynamo/list-tables)


















  ;; Let's take our account atom...


















  (s/describe :account/account)


















  (->> (let [[_ _ ks] (s/describe :account/account)]
         ks)
       (map (fn [spec] [spec (s/describe spec)]))
       (into {}))

















  ;; ...and put it into the cloud!
















  (dynamo/create-table
   :table-name "kth-clj-finance-accounts"
   :attribute-definitions [{:attribute-name "id"
                            :attribute-type "S"}]
   :key-schema [{:attribute-name "id"
                 :key-type "HASH"}]
   :billing-mode "PAY_PER_REQUEST")
















  (dynamo/list-tables)
















  (def accounts-table "kth-clj-finance-accounts")
















  ;; SELECT * FROM accounts
















  (dynamo/scan :table-name "kth-clj-finance-accounts")
















  ;; Let's put some data in there!
































  (dynamo/put-item
   :table-name accounts-table
   :item (sgen/generate (s/gen :account/account)))

















  ;; What went wrong?



















  (sgen/generate (s/gen :account/id))



















  ;; spec is telling us that it tried 100 times to
  ;; generate data that matched our :account/id

















  (s/def :account/id
    (s/with-gen
      (s/and string?
             (partial re-matches #"[0-9]{7}"))
      #(sgen/return (accounts/new-account-number))))

















  (sgen/generate (s/gen :account/id))

















  (s/def :account/date-opened
    (s/with-gen
      (s/and string?
             (partial re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z"))
      #(sgen/return (str (Instant/now)))))


















  (def generate (comp sgen/generate s/gen))

















  (generate :account/account)

















  (dynamo/put-item :table-name accounts-table
                   :item (generate :account/account))


















  (dynamo/scan :table-name accounts-table)
















  (cheat "70d1f1f80133ff9b9e4571a32742c32a504c7bf2")

















  (require '[kth-clj-finance.apigw-handler :as handler])
















  (handler/create-account
   {:body (generate :account/create-request)})

















  (dynamo/scan :table-name accounts-table)
















  (def account-id
    (->> (dynamo/scan :table-name accounts-table)
         :items
         first
         :id))
















  (dynamo/get-item :table-name accounts-table
                   :key {:id {:s account-id}})
















  (require '[kth-clj-finance.db :as db])















  (db/get-account account-id)














  (deploy-lambda)
















  (-> (http/get (str base-url "/accounts/" account-id))
      :body
      (json/parse-string true))
















  (-> (http/post (str base-url "/accounts")
                 {:content-type :json
                  :body (-> (sgen/generate (s/gen :account/create-request))
                            json/generate-string)})
      :body
      (json/parse-string true))
















  (-> (http/get (str base-url "/accounts/" (:id *1)))
      :body
      (json/parse-string true))
















  ;; How can we see all of the accounts?















  (db/list-accounts)















  (-> (http/get (str base-url "/accounts"))
      :body
      (json/parse-string true))

















  (s/valid? :accounts/list *1)

















  ;; Time to move our ledger to the cloud!















  (s/describe :transfer/transfer)
















  (dynamo/create-table
   :table-name "kth-clj-finance-ledger"
   :attribute-definitions
   [{:attribute-name "id"
     :attribute-type "S"}
    {:attribute-name "debit-account"
     :attribute-type "S"}
    {:attribute-name "credit-account"
     :attribute-type "S"}]
   :key-schema [{:attribute-name "id"
                 :key-type "HASH"}]
   :global-secondary-indexes
   [{:index-name "debit-account"
     :key-schema [{:attribute-name "debit-account"
                   :key-type "HASH"}
                  {:attribute-name "id"
                   :key-type "RANGE"}]
     :projection {:projection-type "ALL"}}
    {:index-name "credit-account"
     :key-schema [{:attribute-name "credit-account"
                   :key-type "HASH"}
                  {:attribute-name "id"
                   :key-type "RANGE"}]
     :projection {:projection-type "ALL"}}]
   :billing-mode "PAY_PER_REQUEST")















  ;; Let's try to move some money!















  (def ledger-table "kth-clj-finance-ledger")















  (dynamo/put-item :table-name ledger-table
                   :item (generate :transfer/transfer))















  ;; That was kinda expected. We have a similar
  ;; issue to the one with the account: dates.
















  (s/def :date/iso8601
    (s/with-gen
      (s/and string?
             (partial re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z"))
      #(sgen/return (str (Instant/now)))))

















  (s/def :account/date-opened :date/iso8601)

















  (s/def :transfer/date :date/iso8601)

















  (generate :transfer/transfer)

















  (cheat "HEAD")


















  (handler/create-transfer {:pathParameters {:id "9453577"}
                            :body {:credit-account "8130445"
                                   :amount 4200}})


















  (dynamo/scan :table-name ledger-table)


















  (deploy-lambda)

















  (-> (http/post
       (str base-url "/accounts/" account-id "/transfer")
       {:content-type :json
        :body (-> (sgen/generate (s/gen :transfer/request))
                  json/generate-string)})
      :body
      (json/parse-string true))

















  ;; Where's the money moving?

















  (dynamo/scan :table-name ledger-table)

















  (dynamo/query
   :table-name ledger-table
   :index-name "debit-account"
   :key-condition-expression "#k = :account"
   :expression-attribute-names {"#k" "debit-account"}
   :expression-attribute-values {":account" account-id})

















  (dynamo/query
   :table-name ledger-table
   :index-name "credit-account"
   :key-condition-expression "#k = :account"
   :expression-attribute-names {"#k" "credit-account"}
   :expression-attribute-values {":account" account-id})
















  (defn tx-query [type]
    (let [i (str type "-account")]
      {:table-name ledger-table
       :index-name i
       :key-condition-expression "#k = :account"
       :expression-attribute-names {"#k" i}
       :expression-attribute-values {":account" account-id}}))


















  (defn list-transactions [account-id]
    (->> ["debit" "credit"]
         (map #(->> (dynamo/query (tx-query %))
                    :items))
         (apply concat)))

















  (list-transactions account-id)

















  ;; Show me the money!

















  (defn get-balance [account-id]
    (->> (list-transactions account-id)
         (map (fn [{:keys [debit-account amount]}]
                (if (= account-id debit-account)
                  (* amount -1)
                  amount)))
         (reduce +)))

















  (get-balance account-id)











  














  


  
















  

































  )
