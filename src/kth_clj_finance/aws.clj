(ns kth-clj-finance.aws
  (:require [clojure.string :as string]
            [cheshire.core :as json]
            [kth-clj-finance.accounts :as accounts]
            [amazonica.aws.dynamodbv2 :as dynamo]
            [kth-clj-finance.db :as db]))

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
;; => #'kth-clj-finance.aws/paths

  ;; What do we need for an account?

  (require '[kth-clj-finance.accounts :as accounts])
;; => nil

  (accounts/create-account "Ashley")
;; => {:id "6252825",
;;     :account-holder "Ashley",
;;     :date-opened "2020-11-28T17:03:42.277868Z"}

  ;; Say it with spec!

  (require '[clojure.spec.alpha :as s])
;; => nil

  (s/def :account/id (s/and string?
                            (partial re-matches #"[0-9]{7}")))
;; => :account/id

  (require '[kth-clj-finance.accounts :as accounts])
;; => nil

  (s/valid? :account/id (accounts/new-account-number))
;; => true

  (s/def :account/account-holder string?)
;; => :account/account-holder

  (s/def :account/date-opened (s/and string?
                                     (partial re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z")))
;; => :account/date-opened

  (import '(java.time Instant))
;; => java.time.Instant

  (s/valid? :account/date-opened (str (Instant/now)))
;; => true

  (s/def :account/account (s/keys :req-un [:account/id
                                           :account/account-holder
                                           :account/date-opened]))
;; => :account/account

  (s/valid? :account/account (accounts/create-account "Ashley"))
;; => true

  ;; When creating an account, we only need holder's name

  (s/def :account/create-request (s/keys :req-un [:account/account-holder]))
;; => :account/create-request

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
;; => #'kth-clj-finance.aws/paths

  (s/def :account/accounts (s/coll-of :account/account))
;; => :account/accounts

  (s/def :accounts/list (s/keys :req-un [:account/accounts]))
;; => :accounts/list

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
;; => #'kth-clj-finance.aws/paths

  (s/def :transfer/id string?)
;; => :transfer/id

  (s/def :transfer/debit-account :account/id)
;; => :transfer/debit-account

  (s/def :transfer/credit-account :account/id)
;; => :transfer/credit-account

  (s/def :transfer/amount pos-int?)
;; => :transfer/amount

  (s/def :transfer/date (s/and string?
                               (partial re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z")))
;; => :transfer/date

  (s/def :transfer/transfer (s/keys :req-un [:transfer/id
                                             :transfer/debit-account
                                             :transfer/credit-account
                                             :transfer/amount
                                             :transfer/date]))
;; => :transfer/transfer

  (s/def :transfer/request (s/keys :req-un [:transfer/credit-account
                                            :transfer/amount]))
;; => :transfer/request

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
;; => #'kth-clj-finance.aws/paths

  (require '[amazonica.core :as amazonica])
;; => nil

  (defn refresh-aws-credentials [profile]
    (amazonica/defcredential (amazonica/get-credentials {:profile profile})))
;; => #'kth-clj-finance.aws/refresh-aws-credentials

  (refresh-aws-credentials "jmglov")
;; => #object[com.amazonaws.auth.profile.ProfileCredentialsProvider 0x49567a27 "com.amazonaws.auth.profile.ProfileCredentialsProvider@49567a27"]

  (require '[amazonica.aws.apigateway :as apigw])
;; => nil

  (apigw/get-rest-apis)
;; => {:items []}

  ;; OpenAPI 3 allows us to describe an API with data!

  (require '[kth-clj-finance.openapi :as openapi])
;; => nil

  (require '[clojure.string :as string])
;; => nil

  (defn user-specs []
    (->> (s/registry)
         keys
         (remove #(string/starts-with? (namespace %) "clojure"))))
;; => #'kth-clj-finance.aws/user-specs

  (user-specs)
;; => (:account/account
;;     :transfer/debit-account
;;     :account/id
;;     :account/date-opened
;;     :account/accounts
;;     :transfer/amount
;;     :account/account-holder
;;     :account/create-request
;;     :transfer/request
;;     :accounts/list
;;     :transfer/transfer
;;     :transfer/credit-account)

  (def api
    {:openapi "3.0.0"
     :info {:title "KTH Bank API"
            :version "1.0"}
     :paths (->> paths
                 (map openapi/->path)
                 (into {}))
     :components (openapi/->schemas (user-specs))})
;; => #'kth-clj-finance.aws/api

  api
;; => {:openapi "3.0.0",
;;     :info {:title "KTH Bank API", :version "1.0"},
;;     :paths
;;     {"/accounts"
;;      {:post
;;       {:summary "Create an account",
;;        :responses
;;        {"201"
;;         {:description "Response",
;;          :content
;;          {"application/json"
;;           {:schema {"$ref" "#/components/schemas/AccountAccount"}}}}},
;;        :requestBody
;;        {:content
;;         {"application/json"
;;          {:schema {"$ref" "#/components/schemas/AccountCreateRequest"}}}}},
;;       :get
;;       {:summary "List accounts",
;;        :responses
;;        {"200"
;;         {:description "Response",
;;          :content
;;          {"application/json"
;;           {:schema {"$ref" "#/components/schemas/AccountAccounts"}}}}}}},
;;      "/accounts/{id}"
;;      {:get
;;       {:summary "Get account info",
;;        :responses
;;        {"200"
;;         {:description "Response",
;;          :content
;;          {"application/json"
;;           {:schema {"$ref" "#/components/schemas/AccountAccount"}}}}},
;;        :parameters
;;        [{:name "id",
;;          :in "path",
;;          :required true,
;;          :schema {"$ref" "#/components/schemas/AccountId"}}]}},
;;      "/accounts/{id}/transfer"
;;      {:post
;;       {:summary "Transfer from this account to another",
;;        :responses
;;        {"201"
;;         {:description "Response",
;;          :content
;;          {"application/json"
;;           {:schema {"$ref" "#/components/schemas/TransferTransfer"}}}}},
;;        :parameters
;;        [{:name "id",
;;          :in "path",
;;          :required true,
;;          :schema {"$ref" "#/components/schemas/AccountId"}}],
;;        :requestBody
;;        {:content
;;         {"application/json"
;;          {:schema {"$ref" "#/components/schemas/TransferRequest"}}}}}}},
;;     :components
;;     {:schemas
;;      {"TransferRequest"
;;       {:type "object",
;;        :properties
;;        {"credit-account" {:type "string"},
;;         "amount" {:type "integer", :format "int64", :minimum 1}},
;;        :required ["credit-account" "amount"],
;;        :title "transfer/request"},
;;       "AccountDateOpened" {:type "string"},
;;       "TransferDebitAccount" {:type "string"},
;;       "TransferCreditAccount" {:type "string"},
;;       "AccountCreateRequest"
;;       {:type "object",
;;        :properties {"account-holder" {:type "string"}},
;;        :required ["account-holder"],
;;        :title "account/create-request"},
;;       "TransferAmount" {:type "integer", :format "int64", :minimum 1},
;;       "AccountsList"
;;       {:type "object",
;;        :properties
;;        {"accounts"
;;         {:type "array",
;;          :items
;;          {:type "object",
;;           :properties
;;           {"id" {:type "string"},
;;            "account-holder" {:type "string"},
;;            "date-opened" {:type "string"}},
;;           :required ["id" "account-holder" "date-opened"],
;;           :title "account/account"}}},
;;        :required ["accounts"],
;;        :title "accounts/list"},
;;       "AccountId" {:type "string"},
;;       "AccountAccounts"
;;       {:type "array",
;;        :items
;;        {:type "object",
;;         :properties
;;         {"id" {:type "string"},
;;          "account-holder" {:type "string"},
;;          "date-opened" {:type "string"}},
;;         :required ["id" "account-holder" "date-opened"],
;;         :title "account/account"}},
;;       "AccountAccountHolder" {:type "string"},
;;       "TransferTransfer"
;;       {:type "object",
;;        :properties
;;        {"debit-account" {:type "string"},
;;         "credit-account" {:type "string"},
;;         "amount" {:type "integer", :format "int64", :minimum 1}},
;;        :required ["debit-account" "credit-account" "amount"],
;;        :title "transfer/transfer"},
;;       "AccountAccount"
;;       {:type "object",
;;        :properties
;;        {"id" {:type "string"},
;;         "account-holder" {:type "string"},
;;         "date-opened" {:type "string"}},
;;        :required ["id" "account-holder" "date-opened"],
;;        :title "account/account"}}}}

  (require '[cheshire.core :as json])
;; => nil

  (apigw/import-rest-api :endpoint-configuration-types "REGIONAL"
                         :body (json/generate-string api))
;; => {:api-key-source "HEADER",
;;     :endpoint-configuration {:types ["EDGE"]},
;;     :created-date
;;     #object[org.joda.time.DateTime 0x6346b407 "2020-11-29T08:52:42.000+01:00"],
;;     :version "1.0",
;;     :name "KTH Bank API",
;;     :id "ikt8aiuec5"}

  (def api-id (->> (apigw/get-rest-apis)
                   :items
                   first
                   :id))
;; => #'kth-clj-finance.aws/api-id

  ;; Handle the requests with a lamda function!

  ;; Build the uberjar as per the README

  (require '[amazonica.aws.lambda :as lambda])
;; => nil

  (lambda/list-functions)
;; => {:functions
;;     [{:role
;;       "arn:aws:iam::289341159200:role/service-role/hello-lumo-role-mr29j9h6",
;;       :description "",
;;       :revision-id "f8725f05-e1ce-4af0-b294-ca74ce4f6d4c",
;;       :file-system-configs [],
;;       :code-size 1816,
;;       :function-arn "arn:aws:lambda:eu-west-1:289341159200:function:hello-lumo",
;;       :last-modified "2020-04-24T13:44:52.976+0000",
;;       :code-sha256 "VGYzoPvTjMk+U13suXaXuh/UyrrfletymsLAOUBvPyQ=",
;;       :runtime "provided",
;;       :memory-size 512,
;;       :vpc-config {:subnet-ids [], :security-group-ids [], :vpc-id ""},
;;       :layers
;;       [{:code-size 35860378,
;;         :arn "arn:aws:lambda:eu-west-1:313836948343:layer:lumo-runtime:20"}
;;        {:code-size 7001335,
;;         :arn "arn:aws:lambda:eu-west-1:289341159200:layer:aws-js-sdk:1"}],
;;       :tracing-config {:mode "PassThrough"},
;;       :timeout 5,
;;       :version "$LATEST",
;;       :handler "include-bot.main/handler",
;;       :function-name "hello-lumo"}]}

  (require '[amazonica.aws.s3 :as s3])
;; => nil

  (s3/list-buckets)
;; => [{:creation-date
;;      #object[org.joda.time.DateTime 0x1a0f3338 "2020-06-24T12:02:53.000+02:00"],
;;      :name "jmglov.net",
;;      :owner
;;      {:display-name "jmglov",
;;       :id "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"}}
;;     {:creation-date
;;      #object[org.joda.time.DateTime 0x7641dcd1 "2020-07-03T17:08:09.000+02:00"],
;;      :name "jmglov.net-backups",
;;      :owner
;;      {:display-name "jmglov",
;;       :id "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"}}
;;     {:creation-date
;;      #object[org.joda.time.DateTime 0x5cb3ceef "2020-07-03T17:08:09.000+02:00"],
;;      :name "jmglov.net-home-videos",
;;      :owner
;;      {:display-name "jmglov",
;;       :id "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"}}
;;     {:creation-date
;;      #object[org.joda.time.DateTime 0x17974a9f "2019-10-26T11:19:11.000+02:00"],
;;      :name "misc.jmglov.net",
;;      :owner
;;      {:display-name "jmglov",
;;       :id "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"}}
;;     {:creation-date
;;      #object[org.joda.time.DateTime 0x7fc33b25 "2019-08-02T11:49:08.000+02:00"],
;;      :name "photos.jmglov.net",
;;      :owner
;;      {:display-name "jmglov",
;;       :id "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"}}
;;     {:creation-date
;;      #object[org.joda.time.DateTime 0xa877b9f "2020-07-07T11:36:45.000+02:00"],
;;      :name "sack-kings",
;;      :owner
;;      {:display-name "jmglov",
;;       :id "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"}}
;;     {:creation-date
;;      #object[org.joda.time.DateTime 0x22cd987a "2020-06-25T08:12:20.000+02:00"],
;;      :name "www.jmglov.net",
;;      :owner
;;      {:display-name "jmglov",
;;       :id "022d83ad6361dec3c93757e75c1c3a7982532ffdbf3bf87976490873591e2188"}}]

  (s3/put-object :bucket-name "misc.jmglov.net"
                 :key "kth-clj-finance/kth-clj-finance.jar"
                 :file "target/kth-clj-finance.jar")
;; => {:version-id "kZ3B0p80z2U2459oa8CXdWuDozh2KMZs",
;;     :etag "d11f43745a1392c5383da30957770bc1",
;;     :requester-charged? false,
;;     :content-md5 "0R9DdFoTksU4PaMJV3cLwQ==",
;;     :metadata
;;     {:content-disposition nil,
;;      :expiration-time-rule-id nil,
;;      :user-metadata nil,
;;      :instance-length 0,
;;      :version-id "kZ3B0p80z2U2459oa8CXdWuDozh2KMZs",
;;      :server-side-encryption "AES256",
;;      :server-side-encryption-aws-kms-key-id nil,
;;      :etag "d11f43745a1392c5383da30957770bc1",
;;      :last-modified nil,
;;      :cache-control nil,
;;      :http-expires-date nil,
;;      :content-length 0,
;;      :content-type nil,
;;      :restore-expiration-time nil,
;;      :content-encoding nil,
;;      :expiration-time nil,
;;      :content-md5 nil,
;;      :ongoing-restore nil}}

  (s3/list-objects-v2 :bucket-name "misc.jmglov.net"
                      :prefix "kth-clj-finance/")
;; => {:truncated? false,
;;     :bucket-name "misc.jmglov.net",
;;     :max-keys 1000,
;;     :object-summaries
;;     [{:bucket-name "misc.jmglov.net",
;;       :etag "d11f43745a1392c5383da30957770bc1",
;;       :storage-class "STANDARD",
;;       :last-modified
;;       #object[org.joda.time.DateTime 0x13e5b0f0 "2020-11-29T12:25:20.000+01:00"],
;;       :key "kth-clj-finance/kth-clj-finance.jar",
;;       :size 6081211}],
;;     :common-prefixes [],
;;     :key-count 1,
;;     :prefix "kth-clj-finance/"}

  (require '[amazonica.aws.lambda :as lambda])
;; => nil

  (lambda/create-function :function-name "kth-clj-finance-apigw-handler"
                          :handler "apigw-handler"
                          :runtime "java11"
                          :memory-size 1024
                          :role "arn:aws:iam::289341159200:role/kth-clj-finance-apigw-handler"
                          :code {:s3-bucket "misc.jmglov.net"
                                 :s3-key "kth-clj-finance/kth-clj-finance.jar"})
;; => {:role "arn:aws:iam::289341159200:role/kth-clj-finance-apigw-handler",
;;     :description "uploaded via amazonica",
;;     :revision-id "a6b241b4-28b0-4f77-8bd6-4aaf9b43b6fa",
;;     :file-system-configs [],
;;     :code-size 6081211,
;;     :function-arn
;;     "arn:aws:lambda:eu-west-1:289341159200:function:kth-clj-finance-apigw-handler",
;;     :last-update-status "Successful",
;;     :state "Active",
;;     :last-modified "2020-11-29T11:34:23.524+0000",
;;     :code-sha256 "Uh1cQ8E9Lr9hY3GhJw5Oswgu7pWwgc7pcFm/JgV+Rcg=",
;;     :runtime "java11",
;;     :memory-size 1024,
;;     :layers [],
;;     :tracing-config {:mode "PassThrough"},
;;     :timeout 10,
;;     :version "$LATEST",
;;     :handler "apigw-handler",
;;     :function-name "kth-clj-finance-apigw-handler"}

  (def apigw-handler-lambda (lambda/get-function :function-name "kth-clj-finance-apigw-handler"))
;; => #'kth-clj-finance.aws/apigw-handler-lambda

  apigw-handler-lambda
;; => {:code
;;     {:repository-type "S3",
;;      :location
;;      "https://awslambda-eu-west-1-tasks.s3.eu-west-1.amazonaws.com/snapshots/289341159200/kth-clj-finance-apigw-handler-0efad7c9-b144-45f6-a4a5-b883b04b2b0d?versionId=SXfjO016hxd69yrOOWsPtqjL94P..AyC&X-Amz-Security-Token=IQoJb3JpZ2luX2VjENP%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCWV1LXdlc3QtMSJHMEUCIEO9D7DDEzBAOlG1BHKY1LxGUqneF3vmyKx%2FiGXQy4%2FDAiEAwfdBohlkev%2FSm%2BtwU3lCwP52l%2BKFpGOOnF5%2FRo9whn0qtAMIXBACGgw5NTQzNjkwODI1MTEiDAuRIQxM5wcDd0zY6iqRA0QeoccAvPX4QXiexOyrZ6l6vi2N2aUfsUeYTcfccrqfhasgdKcrQrnCD8ofzSSkREF2%2BY2pFghaYMbfqvRRJqM8Q2PO0e%2BBw05OCyQdBWTsAtLDnzCepdGn3HFJRqzA90FMo8ZY9N3mqtVyu8fhmjI0Ea0Q7L%2Fl3NyhLmwovxjfD4jJbiG%2BiXZUPrtrQZzemc1Z0kA1CzyiMffw9i6%2BeZXEnIdzVnpRwFEn2yGNSvA3elCUAuqujo9bej7JmLzQlCGhLjo1Q00kzXyyLmQB6ebumKnsH8xskqhlGeuFiQTPjtwemrRnJ0NKfULLG5DXq4PqmNIi9qKAqBnhnFwKt%2B41tp6fl3qhYAYfyM5jp1VUmW%2Bd1HBHjqs0GlBpUii1cxBbn3Z45%2BInlV0Hzgr80eNKAKT6tVswtDXNtchqHi75FEd1gMt5qhhZNW25AIOoEs0FXsRlu1t6NvupIS%2FTCN86pHn9j9MYb0dRxsNWp7CgEOJXTmcBQyBj7DDPv9daQptlpio5jgP8X%2FW38kPOlp2LMLuDjv4FOusBYvuSnyCxOFY3ZUyhydWELToAFfFysWCF05OHPSTWGOMZZ4znN7GIZkPHYyBAw9pV%2BohEWtVygZaJ09i8t%2FX0UW6wL9Y6jP9Qr5b%2BSmlYmpJvZvBQDsdMdRDPH7NCpsNcHJEQHa9uO0oHyaosm0retds1ySQmY2QuwpC4FQvhTdKCASfm9bjLokaPh%2FTu7wk2q44QfHomZkvmlZI0MQcJQ47WWf4dcR3mHJApseeDD6flyTlbTBTiepIib20buhVwlVUQ6EGW%2FGUkHkMRj53Kavri%2BEUThDTm2X%2BKrEj8%2FIq4BowIe3TkRw6cKg%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20201129T114804Z&X-Amz-SignedHeaders=host&X-Amz-Expires=600&X-Amz-Credential=ASIA54NGUQSHXRT46VUR%2F20201129%2Feu-west-1%2Fs3%2Faws4_request&X-Amz-Signature=58547ef4d7a49a23359414ce7f3868f39c5eac72d809fc5efb5d858d6f9e3edf"},
;;     :configuration
;;     {:role "arn:aws:iam::289341159200:role/kth-clj-finance-apigw-handler",
;;      :description "uploaded via amazonica",
;;      :revision-id "25c7b273-0164-49bb-a6d3-920a78e24e47",
;;      :file-system-configs [],
;;      :code-size 6081430,
;;      :function-arn
;;      "arn:aws:lambda:eu-west-1:289341159200:function:kth-clj-finance-apigw-handler",
;;      :last-update-status "Successful",
;;      :state "Active",
;;      :last-modified "2020-11-29T11:45:40.190+0000",
;;      :code-sha256 "vSsbJtOI+Z98FK/bLUuGGpT0mmaZqyG6x74XdMhQMKI=",
;;      :runtime "java11",
;;      :memory-size 1024,
;;      :layers [],
;;      :tracing-config {:mode "PassThrough"},
;;      :timeout 10,
;;      :version "$LATEST",
;;      :handler "apigw-handler",
;;      :function-name "kth-clj-finance-apigw-handler"}}

  (def apigw-handler-function (get-in apigw-handler-lambda [:configuration :function-name]))
;; => #'kth-clj-finance.aws/apigw-handler-function

  (lambda/invoke :function-name apigw-handler-function
                 :payload (slurp "resources/apigw-test-request.json"))
;; => {:status-code 200,
;;     :executed-version "$LATEST",
;;     :payload
;;     #object[java.nio.HeapByteBuffer 0x43358b53 "java.nio.HeapByteBuffer[pos=0 lim=15 cap=15]"]}

  (require '[amazonica.aws.logs :as logs])
;; => nil

  (logs/describe-log-groups)
;; => {:log-groups
;;     [{:arn
;;       "arn:aws:logs:eu-west-1:289341159200:log-group:/aws/lambda/hello-lumo:*",
;;       :log-group-name "/aws/lambda/hello-lumo",
;;       :metric-filter-count 0,
;;       :stored-bytes 8971,
;;       :creation-time 1587731968811}
;;      {:arn
;;       "arn:aws:logs:eu-west-1:289341159200:log-group:/aws/lambda/kth-clj-finance-apigw-handler:*",
;;       :log-group-name "/aws/lambda/kth-clj-finance-apigw-handler",
;;       :metric-filter-count 0,
;;       :stored-bytes 0,
;;       :creation-time 1606649732607}]}

  (logs/describe-log-streams :log-group-name "/aws/lambda/kth-clj-finance-apigw-handler")
;; => {:log-streams
;;     [{:arn
;;       "arn:aws:logs:eu-west-1:289341159200:log-group:/aws/lambda/kth-clj-finance-apigw-handler:log-stream:2020/11/29/[$LATEST]12a9ce86ca214978bae8f34e0f62e509",
;;       :log-stream-name "2020/11/29/[$LATEST]12a9ce86ca214978bae8f34e0f62e509",
;;       :last-event-timestamp 1606649724109,
;;       :last-ingestion-time 1606649732638,
;;       :upload-sequence-token
;;       "49610383047926255837943540421765696693853052720403101250",
;;       :first-event-timestamp 1606649723414,
;;       :stored-bytes 0,
;;       :creation-time 1606649732631}
;;      {:arn
;;       "arn:aws:logs:eu-west-1:289341159200:log-group:/aws/lambda/kth-clj-finance-apigw-handler:log-stream:2020/11/29/[$LATEST]6a1c5d07009f47e19bd8238b17d579f1",
;;       :log-stream-name "2020/11/29/[$LATEST]6a1c5d07009f47e19bd8238b17d579f1",
;;       :last-event-timestamp 1606650618800,
;;       :last-ingestion-time 1606650627550,
;;       :upload-sequence-token
;;       "49605473494938331280165799569383578905038459616761288226",
;;       :first-event-timestamp 1606650618447,
;;       :stored-bytes 0,
;;       :creation-time 1606650627542}]}

  (logs/get-log-events :log-group-name "/aws/lambda/kth-clj-finance-apigw-handler"
                       :log-stream-name
                       (->> (logs/describe-log-streams :log-group-name "/aws/lambda/kth-clj-finance-apigw-handler")
                            :log-streams
                            last
                            :log-stream-name))
;; => {:next-forward-token
;;     "f/35829506072920354523040283171798582073964091745067204611",
;;     :next-backward-token
;;     "b/35829506065048191467958973202836473523719220133456117760",
;;     :events
;;     [{:ingestion-time 1606650627550,
;;       :timestamp 1606650618447,
;;       :message
;;       "START RequestId: 06c4a5d2-24d4-4c27-8caf-f09b3ddb0a90 Version: $LATEST\n"}
;;      {:ingestion-time 1606650627550,
;;       :timestamp 1606650618780,
;;       :message
;;       "{:path \"/path/to/resource\", :queryStringParameters {:foo \"bar\"}, :pathParameters {:proxy \"/path/to/resource\"}, :headers {:Upgrade-Insecure-Requests \"1\", :X-Amz-Cf-Id \"cDehVQoZnx43VYQb9j2-nvCh-9z396Uhbp027Y2JvkCPNLmGJHqlaA==\", :CloudFront-Is-Tablet-Viewer \"false\", :CloudFront-Forwarded-Proto \"https\", :X-Forwarded-Proto \"https\", :X-Forwarded-Port \"443\", :Accept \"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\", :Accept-Encoding \"gzip, deflate, sdch\", :X-Forwarded-For \"127.0.0.1, 127.0.0.2\", :CloudFront-Viewer-Country \"US\", :Accept-Language \"en-US,en;q=0.8\", :Cache-Control \"max-age=0\", :CloudFront-Is-Desktop-Viewer \"true\", :Via \"1.1 08f323deadbeefa7af34d5feb414ce27.cloudfront.net (CloudFront)\", :CloudFront-Is-SmartTV-Viewer \"false\", :CloudFront-Is-Mobile-Viewer \"false\", :Host \"1234567890.execute-api.eu-west-1.amazonaws.com\", :User-Agent \"Custom User Agent String\"}, :stageVariables {:baz \"qux\"}, :resource \"/{proxy+}\", :isBase64Encoded true, :multiValueQueryStringParameters {:foo [\"bar\"]}, :httpMethod \"POST\", :requestContext {:path \"/prod/path/to/resource\", :identity {:caller nil, :sourceIp \"127.0.0.1\", :cognitoIdentityId nil, :userAgent \"Custom User Agent String\", :cognitoAuthenticationProvider nil, :accessKey nil, :accountId nil, :user nil, :cognitoAuthenticationType nil, :cognitoIdentityPoolId nil, :userArn nil}, :stage \"prod\", :protocol \"HTTP/1.1\", :resourcePath \"/{proxy+}\", :resourceId \"123456\", :requestTime \"09/Apr/2015:12:34:56 +0000\", :requestId \"c6af9ac6-7b61-11e6-9a41-93e8deadbeef\", :httpMethod \"POST\", :requestTimeEpoch 1428582896000, :accountId \"123456789012\", :apiId \"1234567890\"}, :body \"eyJ0ZXN0IjoiYm9keSJ9\", :multiValueHeaders {:Upgrade-Insecure-Requests [\"1\"], :X-Amz-Cf-Id [\"cDehVQoZnx43VYQb9j2-nvCh-9z396Uhbp027Y2JvkCPNLmGJHqlaA==\"], :CloudFront-Is-Tablet-Viewer [\"false\"], :CloudFront-Forwarded-Proto [\"https\"], :X-Forwarded-Proto [\"https\"], :X-Forwarded-Port [\"443\"], :Accept [\"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\"], :Accept-Encoding [\"gzip, deflate, sdch\"], :X-Forwarded-For [\"127.0.0.1, 127.0.0.2\"], :CloudFront-Viewer-Country [\"US\"], :Accept-Language [\"en-US,en;q=0.8\"], :Cache-Control [\"max-age=0\"], :CloudFront-Is-Desktop-Viewer [\"true\"], :Via [\"1.1 08f323deadbeefa7af34d5feb414ce27.cloudfront.net (CloudFront)\"], :CloudFront-Is-SmartTV-Viewer [\"false\"], :CloudFront-Is-Mobile-Viewer [\"false\"], :Host [\"0123456789.execute-api.eu-west-1.amazonaws.com\"], :User-Agent [\"Custom User Agent String\"]}}\n"}
;;      {:ingestion-time 1606650627550,
;;       :timestamp 1606650618800,
;;       :message "END RequestId: 06c4a5d2-24d4-4c27-8caf-f09b3ddb0a90\n"}
;;      {:ingestion-time 1606650627550,
;;       :timestamp 1606650618800,
;;       :message
;;       "REPORT RequestId: 06c4a5d2-24d4-4c27-8caf-f09b3ddb0a90\tDuration: 352.56 ms\tBilled Duration: 400 ms\tMemory Size: 1024 MB\tMax Memory Used: 142 MB\tInit Duration: 2874.89 ms\t\n"}]}

  (defn get-log-events [function-name]
    (let [log-group-name (str "/aws/lambda/" function-name)]
      (logs/get-log-events :log-group-name log-group-name
                           :log-stream-name
                           (->> (logs/describe-log-streams :log-group-name log-group-name)
                                :log-streams
                                last
                                :log-stream-name))))
;; => #'kth-clj-finance.aws/get-log-events

  (def api
    {:openapi "3.0.0"
     :info {:title "KTH Bank API"
            :version "1.0"}
     :paths (->> paths
                 (map (partial openapi/->path apigw-handler-function))
                 (into {}))
     :components (openapi/->schemas (user-specs))})
;; => #'kth-clj-finance.aws/api

  api
;; => {:openapi "3.0.0",
;;     :info {:title "KTH Bank API", :version "1.0"},
;;     :paths
;;     {"/accounts"
;;      {:post
;;       {:summary "Create an account",
;;        :responses
;;        {"201"
;;         {:description "Response",
;;          :content
;;          {"application/json"
;;           {:schema {"$ref" "#/components/schemas/AccountAccount"}}}}},
;;        :requestBody
;;        {:content
;;         {"application/json"
;;          {:schema {"$ref" "#/components/schemas/AccountCreateRequest"}}}},
;;        :x-amazon-apigateway-integration
;;        {:type "aws_proxy",
;;         :httpMethod "POST",
;;         :uri
;;         "arn:aws:apigateway:eu-west-1:lambda:path/2015-03-31/functions/arn:aws:lambda:eu-west-1:289341159200:function:kth-clj-finance-apigw-handler/invocations",
;;         :credentials "arn:aws:iam::289341159200:role/kth-clj-finance-apigw"}},
;;       :get
;;       {:summary "List accounts",
;;        :responses
;;        {"200"
;;         {:description "Response",
;;          :content
;;          {"application/json"
;;           {:schema {"$ref" "#/components/schemas/AccountAccounts"}}}}},
;;        :x-amazon-apigateway-integration
;;        {:type "aws_proxy",
;;         :httpMethod "POST",
;;         :uri
;;         "arn:aws:apigateway:eu-west-1:lambda:path/2015-03-31/functions/arn:aws:lambda:eu-west-1:289341159200:function:kth-clj-finance-apigw-handler/invocations",
;;         :credentials "arn:aws:iam::289341159200:role/kth-clj-finance-apigw"}}},
;;      "/accounts/{id}"
;;      {:get
;;       {:summary "Get account info",
;;        :responses
;;        {"200"
;;         {:description "Response",
;;          :content
;;          {"application/json"
;;           {:schema {"$ref" "#/components/schemas/AccountAccount"}}}}},
;;        :parameters
;;        [{:name "id",
;;          :in "path",
;;          :required true,
;;          :schema {"$ref" "#/components/schemas/AccountId"}}],
;;        :x-amazon-apigateway-integration
;;        {:type "aws_proxy",
;;         :httpMethod "POST",
;;         :uri
;;         "arn:aws:apigateway:eu-west-1:lambda:path/2015-03-31/functions/arn:aws:lambda:eu-west-1:289341159200:function:kth-clj-finance-apigw-handler/invocations",
;;         :credentials "arn:aws:iam::289341159200:role/kth-clj-finance-apigw"}}},
;;      "/accounts/{id}/transfer"
;;      {:post
;;       {:summary "Transfer from this account to another",
;;        :responses
;;        {"201"
;;         {:description "Response",
;;          :content
;;          {"application/json"
;;           {:schema {"$ref" "#/components/schemas/TransferTransfer"}}}}},
;;        :parameters
;;        [{:name "id",
;;          :in "path",
;;          :required true,
;;          :schema {"$ref" "#/components/schemas/AccountId"}}],
;;        :requestBody
;;        {:content
;;         {"application/json"
;;          {:schema {"$ref" "#/components/schemas/TransferRequest"}}}},
;;        :x-amazon-apigateway-integration
;;        {:type "aws_proxy",
;;         :httpMethod "POST",
;;         :uri
;;         "arn:aws:apigateway:eu-west-1:lambda:path/2015-03-31/functions/arn:aws:lambda:eu-west-1:289341159200:function:kth-clj-finance-apigw-handler/invocations",
;;         :credentials "arn:aws:iam::289341159200:role/kth-clj-finance-apigw"}}}},
;;     :components
;;     {:schemas
;;      {"TransferRequest"
;;       {:type "object",
;;        :properties
;;        {"credit-account" {:type "string"},
;;         "amount" {:type "integer", :format "int64", :minimum 1}},
;;        :required ["credit-account" "amount"],
;;        :title "transfer/request"},
;;       "AccountDateOpened" {:type "string"},
;;       "TransferDebitAccount" {:type "string"},
;;       "TransferCreditAccount" {:type "string"},
;;       "AccountCreateRequest"
;;       {:type "object",
;;        :properties {"account-holder" {:type "string"}},
;;        :required ["account-holder"],
;;        :title "account/create-request"},
;;       "TransferAmount" {:type "integer", :format "int64", :minimum 1},
;;       "AccountsList"
;;       {:type "object",
;;        :properties
;;        {"accounts"
;;         {:type "array",
;;          :items
;;          {:type "object",
;;           :properties
;;           {"id" {:type "string"},
;;            "account-holder" {:type "string"},
;;            "date-opened" {:type "string"}},
;;           :required ["id" "account-holder" "date-opened"],
;;           :title "account/account"}}},
;;        :required ["accounts"],
;;        :title "accounts/list"},
;;       "AccountId" {:type "string"},
;;       "AccountAccounts"
;;       {:type "array",
;;        :items
;;        {:type "object",
;;         :properties
;;         {"id" {:type "string"},
;;          "account-holder" {:type "string"},
;;          "date-opened" {:type "string"}},
;;         :required ["id" "account-holder" "date-opened"],
;;         :title "account/account"}},
;;       "AccountAccountHolder" {:type "string"},
;;       "TransferTransfer"
;;       {:type "object",
;;        :properties
;;        {"debit-account" {:type "string"},
;;         "credit-account" {:type "string"},
;;         "amount" {:type "integer", :format "int64", :minimum 1}},
;;        :required ["debit-account" "credit-account" "amount"],
;;        :title "transfer/transfer"},
;;       "AccountAccount"
;;       {:type "object",
;;        :properties
;;        {"id" {:type "string"},
;;         "account-holder" {:type "string"},
;;         "date-opened" {:type "string"}},
;;        :required ["id" "account-holder" "date-opened"],
;;        :title "account/account"}}}}

  (apigw/put-rest-api :rest-api-id api-id
                      :body (json/generate-string api))
;; => {:created-date
;;     #object[org.joda.time.DateTime 0x52b87d3d "2020-11-29T08:52:42.000+01:00"],
;;     :endpoint-configuration {:types ["EDGE"]},
;;     :api-key-source "HEADER",
;;     :version "1.0",
;;     :name "KTH Bank API",
;;     :id "ikt8aiuec5"}

  (require '[clj-http.client :as client])
;; => nil

  (apigw/get-rest-apis)
;; => {:items
;;     [{:api-key-source "HEADER",
;;       :endpoint-configuration {:types ["EDGE"]},
;;       :created-date
;;       #object[org.joda.time.DateTime 0x29b99242 "2020-11-29T08:52:42.000+01:00"],
;;       :version "1.0",
;;       :name "KTH Bank API",
;;       :id "ikt8aiuec5"}]}

  (def apigw (->> (apigw/get-rest-apis)
                  :items
                  first))
;; => #'kth-clj-finance.aws/apigw

  apigw
;; => {:api-key-source "HEADER",
;;     :endpoint-configuration {:types ["EDGE"]},
;;     :created-date
;;     #object[org.joda.time.DateTime 0xc3522a8 "2020-11-29T08:52:42.000+01:00"],
;;     :version "1.0",
;;     :name "KTH Bank API",
;;     :id "ikt8aiuec5"}

  (apigw/create-deployment :rest-api-id (:id apigw))
;; => {:created-date
;;     #object[org.joda.time.DateTime 0x5d714a2a "2020-11-29T13:37:01.000+01:00"],
;;     :id "n0qx0s"}

  (apigw/create-stage :rest-api-id (:id apigw)
                      :stage-name "api"
                      :deployment-id "n0qx0s")
;; => {:deployment-id "n0qx0s",
;;     :stage-name "api",
;;     :cache-cluster-enabled false,
;;     :tracing-enabled false,
;;     :created-date
;;     #object[org.joda.time.DateTime 0x538c8fdd "2020-11-29T13:38:09.000+01:00"],
;;     :cache-cluster-status "NOT_AVAILABLE",
;;     :last-updated-date
;;     #object[org.joda.time.DateTime 0x74ddb2e "2020-11-29T13:38:09.000+01:00"]}

  (apigw/get-stage :rest-api-id (:id apigw)
                   :stage-name "api")
;; => {:deployment-id "n0qx0s",
;;     :stage-name "api",
;;     :cache-cluster-enabled false,
;;     :tracing-enabled false,
;;     :created-date
;;     #object[org.joda.time.DateTime 0x6398f59f "2020-11-29T13:38:09.000+01:00"],
;;     :cache-cluster-status "NOT_AVAILABLE",
;;     :last-updated-date
;;     #object[org.joda.time.DateTime 0x3890381f "2020-11-29T13:38:09.000+01:00"]}

  (def base-url (format "https://%s.execute-api.eu-west-1.amazonaws.com/api"
                        (:id apigw)))
;; => #'kth-clj-finance.aws/base-url

  (require '[clj-http.client :as http])
;; => nil

  (http/get (str base-url "/accounts"))

  (get-log-events apigw-handler-function)
;; => {:events
;;     [{:ingestion-time 1606653671718,
;;       :timestamp 1606653662613,
;;       :message
;;       "START RequestId: a307a43b-6287-4d09-aa31-7c1e51264295 Version: $LATEST\n"}
;;      {:ingestion-time 1606653671718,
;;       :timestamp 1606653663266,
;;       :message
;;       "{:path \"/accounts\", :queryStringParameters nil, :pathParameters nil, :headers {:X-Amz-Cf-Id \"OdZUA2HmgveAWJZc_c_1GbUXCWh287BTex5shUiH1FuG2lRIDQ0U9A==\", :CloudFront-Is-Tablet-Viewer \"false\", :CloudFront-Forwarded-Proto \"https\", :X-Forwarded-Proto \"https\", :X-Forwarded-Port \"443\", :Accept-Encoding \"gzip, deflate\", :X-Forwarded-For \"213.89.143.175, 130.176.149.141\", :CloudFront-Viewer-Country \"SE\", :CloudFront-Is-Desktop-Viewer \"true\", :Via \"1.1 2f7792bdc67f7953e2dce93aea1bb9ee.cloudfront.net (CloudFront)\", :CloudFront-Is-SmartTV-Viewer \"false\", :CloudFront-Is-Mobile-Viewer \"false\", :Host \"ikt8aiuec5.execute-api.eu-west-1.amazonaws.com\", :User-Agent \"Apache-HttpClient/4.5.10 (Java/11.0.6)\", :X-Amzn-Trace-Id \"Root=1-5fc396db-3d8c2d4f3c3c83455e5e25fc\"}, :stageVariables nil, :resource \"/accounts\", :isBase64Encoded false, :multiValueQueryStringParameters nil, :httpMethod \"GET\", :requestContext {:path \"/api/accounts\", :identity {:caller nil, :sourceIp \"213.89.143.175\", :principalOrgId nil, :cognitoIdentityId nil, :userAgent \"Apache-HttpClient/4.5.10 (Java/11.0.6)\", :cognitoAuthenticationProvider nil, :accessKey nil, :accountId nil, :user nil, :cognitoAuthenticationType nil, :cognitoIdentityPoolId nil, :userArn nil}, :stage \"api\", :protocol \"HTTP/1.1\", :resourcePath \"/accounts\", :domainPrefix \"ikt8aiuec5\", :resourceId \"xafp7b\", :requestTime \"29/Nov/2020:12:40:59 +0000\", :requestId \"ca06962a-db2a-40fa-b00e-aaaa6e2c9eef\", :domainName \"ikt8aiuec5.execute-api.eu-west-1.amazonaws.com\", :httpMethod \"GET\", :requestTimeEpoch 1606653659349, :accountId \"289341159200\", :extendedRequestId \"WxSCSGrXDoEFVlQ=\", :apiId \"ikt8aiuec5\"}, :body nil, :multiValueHeaders {:X-Amz-Cf-Id [\"OdZUA2HmgveAWJZc_c_1GbUXCWh287BTex5shUiH1FuG2lRIDQ0U9A==\"], :CloudFront-Is-Tablet-Viewer [\"false\"], :CloudFront-Forwarded-Proto [\"https\"], :X-Forwarded-Proto [\"https\"], :X-Forwarded-Port [\"443\"], :Accept-Encoding [\"gzip, deflate\"], :X-Forwarded-For [\"213.89.143.175, 130.176.149.141\"], :CloudFront-Viewer-Country [\"SE\"], :CloudFront-Is-Desktop-Viewer [\"true\"], :Via [\"1.1 2f7792bdc67f7953e2dce93aea1bb9ee.cloudfront.net (CloudFront)\"], :CloudFront-Is-SmartTV-Viewer [\"false\"], :CloudFront-Is-Mobile-Viewer [\"false\"], :Host [\"ikt8aiuec5.execute-api.eu-west-1.amazonaws.com\"], :User-Agent [\"Apache-HttpClient/4.5.10 (Java/11.0.6)\"], :X-Amzn-Trace-Id [\"Root=1-5fc396db-3d8c2d4f3c3c83455e5e25fc\"]}}\n"}
;;      {:ingestion-time 1606653671718,
;;       :timestamp 1606653663344,
;;       :message "END RequestId: a307a43b-6287-4d09-aa31-7c1e51264295\n"}
;;      {:ingestion-time 1606653671718,
;;       :timestamp 1606653663344,
;;       :message
;;       "REPORT RequestId: a307a43b-6287-4d09-aa31-7c1e51264295\tDuration: 730.28 ms\tBilled Duration: 800 ms\tMemory Size: 1024 MB\tMax Memory Used: 156 MB\tInit Duration: 3020.63 ms\t\n"}
;;      {:ingestion-time 1606653828945,
;;       :timestamp 1606653819925,
;;       :message
;;       "START RequestId: 43c9388a-d7f8-4c6d-bd17-e4aa488d94a8 Version: $LATEST\n"}
;;      {:ingestion-time 1606653828945,
;;       :timestamp 1606653819968,
;;       :message
;;       "{:path \"/accounts\", :queryStringParameters nil, :pathParameters nil, :headers {:X-Amz-Cf-Id \"Zu6HiBLaCGbNTJ3Y6qf4zoCbicy3UnoA2CwC6lmqp-KY4aH0LxdvDw==\", :CloudFront-Is-Tablet-Viewer \"false\", :CloudFront-Forwarded-Proto \"https\", :X-Forwarded-Proto \"https\", :X-Forwarded-Port \"443\", :Accept-Encoding \"gzip, deflate\", :X-Forwarded-For \"213.89.143.175, 130.176.149.165\", :CloudFront-Viewer-Country \"SE\", :CloudFront-Is-Desktop-Viewer \"true\", :Via \"1.1 cb05e10ed4a973b87ff15498c30d269c.cloudfront.net (CloudFront)\", :CloudFront-Is-SmartTV-Viewer \"false\", :CloudFront-Is-Mobile-Viewer \"false\", :Host \"ikt8aiuec5.execute-api.eu-west-1.amazonaws.com\", :User-Agent \"Apache-HttpClient/4.5.10 (Java/11.0.6)\", :X-Amzn-Trace-Id \"Root=1-5fc3977b-69392cd970a317b650f6dc54\"}, :stageVariables nil, :resource \"/accounts\", :isBase64Encoded false, :multiValueQueryStringParameters nil, :httpMethod \"GET\", :requestContext {:path \"/api/accounts\", :identity {:caller nil, :sourceIp \"213.89.143.175\", :principalOrgId nil, :cognitoIdentityId nil, :userAgent \"Apache-HttpClient/4.5.10 (Java/11.0.6)\", :cognitoAuthenticationProvider nil, :accessKey nil, :accountId nil, :user nil, :cognitoAuthenticationType nil, :cognitoIdentityPoolId nil, :userArn nil}, :stage \"api\", :protocol \"HTTP/1.1\", :resourcePath \"/accounts\", :domainPrefix \"ikt8aiuec5\", :resourceId \"xafp7b\", :requestTime \"29/Nov/2020:12:43:39 +0000\", :requestId \"e801d900-ee60-4f36-ac95-da6c7c9628fa\", :domainName \"ikt8aiuec5.execute-api.eu-west-1.amazonaws.com\", :httpMethod \"GET\", :requestTimeEpoch 1606653819864, :accountId \"289341159200\", :extendedRequestId \"WxSbXEA1joEF99Q=\", :apiId \"ikt8aiuec5\"}, :body nil, :multiValueHeaders {:X-Amz-Cf-Id [\"Zu6HiBLaCGbNTJ3Y6qf4zoCbicy3UnoA2CwC6lmqp-KY4aH0LxdvDw==\"], :CloudFront-Is-Tablet-Viewer [\"false\"], :CloudFront-Forwarded-Proto [\"https\"], :X-Forwarded-Proto [\"https\"], :X-Forwarded-Port [\"443\"], :Accept-Encoding [\"gzip, deflate\"], :X-Forwarded-For [\"213.89.143.175, 130.176.149.165\"], :CloudFront-Viewer-Country [\"SE\"], :CloudFront-Is-Desktop-Viewer [\"true\"], :Via [\"1.1 cb05e10ed4a973b87ff15498c30d269c.cloudfront.net (CloudFront)\"], :CloudFront-Is-SmartTV-Viewer [\"false\"], :CloudFront-Is-Mobile-Viewer [\"false\"], :Host [\"ikt8aiuec5.execute-api.eu-west-1.amazonaws.com\"], :User-Agent [\"Apache-HttpClient/4.5.10 (Java/11.0.6)\"], :X-Amzn-Trace-Id [\"Root=1-5fc3977b-69392cd970a317b650f6dc54\"]}}\n"}
;;      {:ingestion-time 1606653828945,
;;       :timestamp 1606653820005,
;;       :message "END RequestId: 43c9388a-d7f8-4c6d-bd17-e4aa488d94a8\n"}
;;      {:ingestion-time 1606653828945,
;;       :timestamp 1606653820005,
;;       :message
;;       "REPORT RequestId: 43c9388a-d7f8-4c6d-bd17-e4aa488d94a8\tDuration: 76.26 ms\tBilled Duration: 100 ms\tMemory Size: 1024 MB\tMax Memory Used: 159 MB\t\n"}],
;;     :next-forward-token
;;     "f/35829577462177387785263740841896570747594470402570518531",
;;     :next-backward-token
;;     "b/35829573952218499498131903159231234894147318978086109184"}

  (def apigw-handler-code {:function-name "kth-clj-finance-apigw-handler"
                           :s3-bucket "misc.jmglov.net"
                           :s3-key "kth-clj-finance/kth-clj-finance.jar"})
;; => #'kth-clj-finance.aws/apigw-handler-code

  (s3/put-object :bucket-name "misc.jmglov.net"
                 :key "kth-clj-finance/kth-clj-finance.jar"
                 :file "target/kth-clj-finance.jar")
;; => {:version-id "EUD3bMJR_J016mLypRcMMwbJZaJLSo_R",
;;     :etag "138b8187ae1761dd2410f26a672ea93a",
;;     :requester-charged? false,
;;     :content-md5 "E4uBh64XYd0kEPJqZy6pOg==",
;;     :metadata
;;     {:content-disposition nil,
;;      :expiration-time-rule-id nil,
;;      :user-metadata nil,
;;      :instance-length 0,
;;      :version-id "EUD3bMJR_J016mLypRcMMwbJZaJLSo_R",
;;      :server-side-encryption "AES256",
;;      :server-side-encryption-aws-kms-key-id nil,
;;      :etag "138b8187ae1761dd2410f26a672ea93a",
;;      :last-modified nil,
;;      :cache-control nil,
;;      :http-expires-date nil,
;;      :content-length 0,
;;      :content-type nil,
;;      :restore-expiration-time nil,
;;      :content-encoding nil,
;;      :expiration-time nil,
;;      :content-md5 nil,
;;      :ongoing-restore nil}}

  (lambda/update-function-code apigw-handler-code)
;; => {:role "arn:aws:iam::289341159200:role/kth-clj-finance-apigw-handler",
;;     :description "uploaded via amazonica",
;;     :revision-id "828f44d8-3302-4861-9003-3e47bc8ffcbc",
;;     :file-system-configs [],
;;     :code-size 6088464,
;;     :function-arn
;;     "arn:aws:lambda:eu-west-1:289341159200:function:kth-clj-finance-apigw-handler",
;;     :last-update-status "Successful",
;;     :state "Active",
;;     :last-modified "2020-11-29T13:24:10.390+0000",
;;     :code-sha256 "PuNkInsbDaezUqU3yXo1Dg6wzB38+/1Bw3j//kyX+hE=",
;;     :runtime "java11",
;;     :memory-size 1024,
;;     :layers [],
;;     :tracing-config {:mode "PassThrough"},
;;     :timeout 10,
;;     :version "$LATEST",
;;     :handler "apigw-handler",
;;     :function-name "kth-clj-finance-apigw-handler"}

  (apigw/create-deployment :rest-api-id (:id apigw)
                           :stage-name "api")
;; => {:created-date
;;     #object[org.joda.time.DateTime 0x392809c3 "2020-11-29T14:25:28.000+01:00"],
;;     :id "kmw5tb"}

  (http/get (str base-url "/accounts"))
;; => {:cached nil,
;;     :request-time 4081,
;;     :repeatable? false,
;;     :protocol-version {:name "HTTP", :major 1, :minor 1},
;;     :streaming? true,
;;     :http-client
;;     #object[org.apache.http.impl.client.InternalHttpClient 0x1f0b439b "org.apache.http.impl.client.InternalHttpClient@1f0b439b"],
;;     :chunked? false,
;;     :reason-phrase "OK",
;;     :headers
;;     {"X-Cache" "Miss from cloudfront",
;;      "x-amzn-RequestId" "7456dd52-b77b-4fc9-add7-ae1fbfde0cdd",
;;      "Via" "1.1 a52c33748955378f279062b7fc7ef91e.cloudfront.net (CloudFront)",
;;      "Content-Type" "application/json",
;;      "Content-Length" "15",
;;      "Connection" "close",
;;      "x-amz-apigw-id" "WxZcjF4_DoEFoUQ=",
;;      "X-Amz-Cf-Pop" "ARN54-C1",
;;      "X-Amzn-Trace-Id" "Root=1-5fc3a2b6-3ef07ed71d2083d24baf015b;Sampled=0",
;;      "Date" "Sun, 29 Nov 2020 13:31:38 GMT",
;;      "X-Amz-Cf-Id" "e0l37yaXDy6bGhA2lJoZBfdrVC5H398iox1ro8xD4EH7tuOWhCJluA=="},
;;     :orig-content-encoding nil,
;;     :status 200,
;;     :length 15,
;;     :body "{\"accounts\":[]}",
;;     :trace-redirects []}

  (-> (http/get (str base-url "/accounts"))
      :body
      (json/parse-string true))
;; => {:accounts []}

  (require '[clojure.spec.gen.alpha :as sgen])
;; => nil

  (sgen/generate (s/gen :account/create-request))
;; => {:account-holder "YyqTWPNREH6WTf"}

  (http/post (str base-url "/accounts")
             {:content-type :json
              :body (-> (sgen/generate (s/gen :account/create-request))
                        json/generate-string)})

  (defn deploy-lambda []
    (s3/put-object :bucket-name "misc.jmglov.net"
                   :key "kth-clj-finance/kth-clj-finance.jar"
                   :file "target/kth-clj-finance.jar")
    (lambda/update-function-code apigw-handler-code)
    (apigw/create-deployment :rest-api-id (:id apigw)
                             :stage-name "api"))
;; => #'kth-clj-finance.aws/deploy-lambda

  (deploy-lambda)
;; => {:created-date
;;     #object[org.joda.time.DateTime 0x2495b0c6 "2020-11-29T14:58:10.000+01:00"],
;;     :id "4iu2o2"}

  (-> (http/post (str base-url "/accounts")
                 {:content-type :json
                  :body (-> (sgen/generate (s/gen :account/create-request))
                            json/generate-string)})
      :body
      (json/parse-string true))
;; => {:id "1314693",
;;     :account-holder "R13DXAr317A6g816wjFx6jE1g5NA9n",
;;     :date-opened "2020-11-29T13:59:42.041854Z"}

  (-> (http/get (str base-url "/accounts/1314693"))
      :body
      (json/parse-string true))

  (deploy-lambda)
;; => {:created-date
;;     #object[org.joda.time.DateTime 0x43b97892 "2020-11-29T15:03:43.000+01:00"],
;;     :id "h1deh2"}

  (-> (http/get (str base-url "/accounts/1314693"))
      :body
      (json/parse-string true))
;; => {:id "1314693"}

  (require '[amazonica.aws.dynamodbv2 :as dynamo])
;; => nil

  (dynamo/list-tables)
;; => {:table-names []}

  (dynamo/create-table :table-name "kth-clj-finance"
                       :attribute-definitions [{:attribute-name "id", :attribute-type "S"}]
                       :key-schema [{:attribute-name "id", :key-type "HASH"}]
                       :billing-mode "PAY_PER_REQUEST")
;; => {:table-description
;;     {:table-id "d120bd10-4f09-408e-a93c-30b9aee06a37",
;;      :key-schema [{:key-type "HASH", :attribute-name "id"}],
;;      :table-size-bytes 0,
;;      :billing-mode-summary {:billing-mode "PAY_PER_REQUEST"},
;;      :attribute-definitions [{:attribute-name "id", :attribute-type "S"}],
;;      :creation-date-time
;;      #object[org.joda.time.DateTime 0x2b5f714f "2020-11-29T15:16:13.942+01:00"],
;;      :item-count 0,
;;      :table-status "CREATING",
;;      :table-name "kth-clj-finance",
;;      :provisioned-throughput
;;      {:read-capacity-units 0,
;;       :write-capacity-units 0,
;;       :number-of-decreases-today 0},
;;      :table-arn "arn:aws:dynamodb:eu-west-1:289341159200:table/kth-clj-finance"}}

  (dynamo/list-tables)
;; => {:table-names ["kth-clj-finance"]}

  (dynamo/scan :table-name "kth-clj-finance")
;; => {:items [], :scanned-count 0, :count 0}

  (dynamo/put-item :table-name "kth-clj-finance"
                   :item (sgen/generate (s/gen :account/account)))

  (sgen/generate (s/gen :account/id))

  (s/def :account/id
    (s/with-gen
      (s/and string?
             (partial re-matches #"[0-9]{7}"))
      #(sgen/return (accounts/new-account-number))))
;; => :account/id

  (sgen/generate (s/gen :account/id))
;; => "2303701"

  (s/def :account/date-opened
    (s/with-gen
      (s/and string?
             (partial re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z"))
      #(sgen/return (str (Instant/now)))))
;; => :account/date-opened

  (def generate (comp sgen/generate s/gen))
;; => #'kth-clj-finance.aws/generate

  (generate :account/account)
;; => {:id "4923111",
;;     :account-holder "X81a1E1UYp1217F4w1vW",
;;     :date-opened "2020-11-29T14:27:07.509479Z"}

  (dynamo/put-item :table-name "kth-clj-finance"
                   :item (generate :account/account))
;; => {}

  (dynamo/scan :table-name "kth-clj-finance")
;; => {:items
;;     [{:date-opened "2020-11-29T14:28:18.092858Z",
;;       :id "5368778",
;;       :account-holder "6WebQ1PT"}],
;;     :scanned-count 1,
;;     :count 1}

  (require '[kth-clj-finance.apigw-handler :as handler])
;; => nil

  (handler/create-account {:body (generate :account/create-request)})
;; => {:id "8130445",
;;     :account-holder "tA9gw",
;;     :date-opened "2020-11-29T14:35:43.137406Z"}

  (dynamo/scan :table-name "kth-clj-finance")
;; => {:items
;;     [{:date-opened "2020-11-29T14:35:18.598211Z",
;;       :id "9453577",
;;       :account-holder "g99E9fiId81drC1CtDcg5Olav9x"}
;;      {:date-opened "2020-11-29T14:28:18.092858Z",
;;       :id "5368778",
;;       :account-holder "6WebQ1PT"}
;;      {:date-opened "2020-11-29T14:35:43.137406Z",
;;       :id "8130445",
;;       :account-holder "tA9gw"}],
;;     :scanned-count 3,
;;     :count 3}

  (dynamo/get-item :table-name "kth-clj-finance"
                   :key {:id {:s "8130445"}})
;; => {:item
;;     {:date-opened "2020-11-29T14:35:43.137406Z",
;;      :id "8130445",
;;      :account-holder "tA9gw"}}

  (db/get-account "8130445")
;; => {:date-opened "2020-11-29T14:35:43.137406Z",
;;     :id "8130445",
;;     :account-holder "tA9gw"}

  (deploy-lambda)
;; => {:created-date
;;     #object[org.joda.time.DateTime 0x4ec851b "2020-11-29T15:46:28.000+01:00"],
;;     :id "bjcv6i"}

  (-> (http/get (str base-url "/accounts/8130445"))
      :body
      (json/parse-string true))
;; => {:date-opened "2020-11-29T14:35:43.137406Z",
;;     :id "8130445",
;;     :account-holder "tA9gw"}

  (-> (http/post (str base-url "/accounts")
                 {:content-type :json
                  :body (-> (sgen/generate (s/gen :account/create-request))
                            json/generate-string)})
      :body
      (json/parse-string true))
;; => {:id "9984638",
;;     :account-holder "iwo63xpG6mEu7IN7FCK2",
;;     :date-opened "2020-11-29T14:51:28.285931Z"}

  (-> (http/get (str base-url "/accounts/9984638"))
      :body
      (json/parse-string true))
;; => {:date-opened "2020-11-29T14:51:28.285931Z",
;;     :id "9984638",
;;     :account-holder "iwo63xpG6mEu7IN7FCK2"}

  (db/list-accounts)
;; => [{:date-opened "2020-11-29T14:35:18.598211Z",
;;      :id "9453577",
;;      :account-holder "g99E9fiId81drC1CtDcg5Olav9x"}
;;     {:date-opened "2020-11-29T14:28:18.092858Z",
;;      :id "5368778",
;;      :account-holder "6WebQ1PT"}
;;     {:date-opened "2020-11-29T14:51:28.285931Z",
;;      :id "9984638",
;;      :account-holder "iwo63xpG6mEu7IN7FCK2"}
;;     {:date-opened "2020-11-29T14:35:43.137406Z",
;;      :id "8130445",
;;      :account-holder "tA9gw"}
;;     {:date-opened "2020-11-29T14:36:31.229388Z",
;;      :id "7771557",
;;      :account-holder "xRlY93lFX"}]

  (deploy-lambda)
;; => {:created-date
;;     #object[org.joda.time.DateTime 0x44b9e299 "2020-11-29T15:58:08.000+01:00"],
;;     :id "v9b9ll"}

  (-> (http/get (str base-url "/accounts"))
      :body
      (json/parse-string true))
;; => {:accounts
;;     [{:date-opened "2020-11-29T14:35:18.598211Z",
;;       :id "9453577",
;;       :account-holder "g99E9fiId81drC1CtDcg5Olav9x"}
;;      {:date-opened "2020-11-29T14:28:18.092858Z",
;;       :id "5368778",
;;       :account-holder "6WebQ1PT"}
;;      {:date-opened "2020-11-29T14:51:28.285931Z",
;;       :id "9984638",
;;       :account-holder "iwo63xpG6mEu7IN7FCK2"}
;;      {:date-opened "2020-11-29T14:35:43.137406Z",
;;       :id "8130445",
;;       :account-holder "tA9gw"}
;;      {:date-opened "2020-11-29T14:36:31.229388Z",
;;       :id "7771557",
;;       :account-holder "xRlY93lFX"}]}

  (s/valid? :accounts/list *1)
;; => true

  paths
;; => {"/accounts"
;;     {:post
;;      {:summary "Create an account",
;;       :request :account/create-request,
;;       :response :account/account},
;;      :get {:summary "List accounts", :response :accounts/list}},
;;     "/accounts/{id}"
;;     {:get
;;      {:summary "Get account info",
;;       :parameter :account/id,
;;       :response :account/account}},
;;     "/accounts/{id}/transfer"
;;     {:post
;;      {:summary "Transfer from this account to another",
;;       :parameter :account/id,
;;       :request :transfer/request,
;;       :response :transfer/transfer}}}

  (generate :transfer/request)
;; => {:credit-account "9518566", :amount 129490}

  (generate :transfer/transfer)

  (s/def :date/iso8601
    (s/with-gen
      (s/and string?
             (partial re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z"))
      #(sgen/return (str (Instant/now)))))
;; => :date/iso8601  

  (s/def :account/date-opened :date/iso8601)
;; => :account/date-opened

  (s/def :transfer/date :date/iso8601)
;; => :transfer/date

  (generate :transfer/transfer)
;; => {:id "5U1gNU6H",
;;     :debit-account "7674925",
;;     :credit-account "9805084",
;;     :amount 7439761,
;;     :date "2020-11-29T15:21:03.558620Z"}

  (handler/create-transfer {:pathParameters {:id "9453577"}
                            :body {:credit-account "8130445"
                                   :amount 4200}})
;; => {:id "9bd39b48-8578-4b32-baab-05054be00f20",
;;     :debit-account "9453577",
;;     :credit-account "8130445",
;;     :amount 4200,
;;     :date "2020-11-29T15:28:11.754935Z"}

  (handler/list-accounts {})
;; => {:accounts
;;     [{:date-opened "2020-11-29T14:35:18.598211Z",
;;       :id "9453577",
;;       :account-holder "g99E9fiId81drC1CtDcg5Olav9x"}
;;      {:date-opened "2020-11-29T14:28:18.092858Z",
;;       :id "5368778",
;;       :account-holder "6WebQ1PT"}
;;      {:date-opened "2020-11-29T14:51:28.285931Z",
;;       :id "9984638",
;;       :account-holder "iwo63xpG6mEu7IN7FCK2"}
;;      {:date-opened "2020-11-29T14:35:43.137406Z",
;;       :id "8130445",
;;       :account-holder "tA9gw"}
;;      {:date-opened "2020-11-29T14:36:31.229388Z",
;;       :id "7771557",
;;       :account-holder "xRlY93lFX"}
;;      {:date "2020-11-29T15:28:11.754935Z",
;;       :amount 4200,
;;       :debit-account "9453577",
;;       :id "9bd39b48-8578-4b32-baab-05054be00f20",
;;       :credit-account "8130445"}]}

  (deploy-lambda)
;; => {:created-date
;;     #object[org.joda.time.DateTime 0x1f03650e "2020-11-29T16:30:18.000+01:00"],
;;     :id "ytc1fe"}

  (-> (http/post (str base-url "/accounts/9453577/transfer")
                 {:content-type :json
                  :body (-> (sgen/generate (s/gen :transfer/request))
                            json/generate-string)})
      :body
      (json/parse-string true))
;; => {:id "cd4581b7-8829-4a6b-b577-510ab8ba1251",
;;     :debit-account "9453577",
;;     :credit-account "1806739",
;;     :amount 874682,
;;     :date "2020-11-29T15:31:46.002351Z"}

  (handler/create-transfer {:pathParameters {:id "9453577"}
                            :body {:credit-account "8130445"
                                   :amount 4200}})
;; => {:id "1a93b13e-3576-450c-bade-eb38b9b6db5e",
;;     :debit-account "9453577",
;;     :credit-account "8130445",
;;     :amount 4200,
;;     :date "2020-11-29T15:37:08.364037Z"}

  (handler/list-accounts {})
;; => {:accounts
;;     [{:date-opened "2020-11-29T14:35:18.598211Z",
;;       :id "9453577",
;;       :account-holder "g99E9fiId81drC1CtDcg5Olav9x"}
;;      {:date-opened "2020-11-29T14:28:18.092858Z",
;;       :id "5368778",
;;       :account-holder "6WebQ1PT"}
;;      {:date "2020-11-29T15:37:08.364037Z",
;;       :amount 4200,
;;       :debit-account "9453577",
;;       :id "TRANSFER:1a93b13e-3576-450c-bade-eb38b9b6db5e",
;;       :credit-account "8130445"}
;;      {:date-opened "2020-11-29T14:51:28.285931Z",
;;       :id "9984638",
;;       :account-holder "iwo63xpG6mEu7IN7FCK2"}
;;      {:date-opened "2020-11-29T14:35:43.137406Z",
;;       :id "8130445",
;;       :account-holder "tA9gw"}
;;      {:date-opened "2020-11-29T14:36:31.229388Z",
;;       :id "7771557",
;;       :account-holder "xRlY93lFX"}
;;      {:date "2020-11-29T15:28:11.754935Z",
;;       :amount 4200,
;;       :debit-account "9453577",
;;       :id "9bd39b48-8578-4b32-baab-05054be00f20",
;;       :credit-account "8130445"}
;;      {:date "2020-11-29T15:31:46.002351Z",
;;       :amount 874682,
;;       :debit-account "9453577",
;;       :id "cd4581b7-8829-4a6b-b577-510ab8ba1251",
;;       :credit-account "1806739"}]}

  (->> (range 5)
       (map (fn [_] (generate :account/create-request)))
       (map #(handler/create-account {:body %})))
;; => Creating account for: FzucV1fj
;;    {"msg":"Account created","data":{"id":"8447824","account-holder":"FzucV1fj","date-opened":"2020-11-29T15:45:26.458461Z"}}
;;    Creating account for: XW7RgZ
;;    {"msg":"Account created","data":{"id":"7235070","account-holder":"XW7RgZ","date-opened":"2020-11-29T15:45:26.722964Z"}}
;;    Creating account for: O5DgS5H1
;;    {"msg":"Account created","data":{"id":"9419353","account-holder":"O5DgS5H1","date-opened":"2020-11-29T15:45:26.789243Z"}}
;;    Creating account for: 4NZ7t7ab60wRw
;;    {"msg":"Account created","data":{"id":"2308349","account-holder":"4NZ7t7ab60wRw","date-opened":"2020-11-29T15:45:26.849187Z"}}
;;    Creating account for: 3498HO
;;    {"msg":"Account created","data":{"id":"3316402","account-holder":"3498HO","date-opened":"2020-11-29T15:45:26.903345Z"}}
;;    ({:id "8447824",
;;      :account-holder "FzucV1fj",
;;      :date-opened "2020-11-29T15:45:26.458461Z"}
;;     {:id "7235070",
;;      :account-holder "XW7RgZ",
;;      :date-opened "2020-11-29T15:45:26.722964Z"}
;;     {:id "9419353",
;;      :account-holder "O5DgS5H1",
;;      :date-opened "2020-11-29T15:45:26.789243Z"}
;;     {:id "2308349",
;;      :account-holder "4NZ7t7ab60wRw",
;;      :date-opened "2020-11-29T15:45:26.849187Z"}
;;     {:id "3316402",
;;      :account-holder "3498HO",
;;      :date-opened "2020-11-29T15:45:26.903345Z"})

  (handler/list-accounts {})
;; => {:accounts
;;     [{:date-opened "2020-11-29T15:45:26.458461Z",
;;       :id "ACCOUNT:8447824",
;;       :account-holder "FzucV1fj"}
;;      {:date-opened "2020-11-29T15:45:26.849187Z",
;;       :id "ACCOUNT:2308349",
;;       :account-holder "4NZ7t7ab60wRw"}
;;      {:date-opened "2020-11-29T15:45:26.789243Z",
;;       :id "ACCOUNT:9419353",
;;       :account-holder "O5DgS5H1"}
;;      {:date-opened "2020-11-29T15:45:26.903345Z",
;;       :id "ACCOUNT:3316402",
;;       :account-holder "3498HO"}
;;      {:date-opened "2020-11-29T15:45:26.722964Z",
;;       :id "ACCOUNT:7235070",
;;       :account-holder "XW7RgZ"}]}

  (handler/list-accounts {})
;; => {:accounts
;;     ({:date-opened "2020-11-29T15:45:26.458461Z",
;;       :id "8447824",
;;       :account-holder "FzucV1fj"}
;;      {:date-opened "2020-11-29T15:45:26.849187Z",
;;       :id "2308349",
;;       :account-holder "4NZ7t7ab60wRw"}
;;      {:date-opened "2020-11-29T15:45:26.789243Z",
;;       :id "9419353",
;;       :account-holder "O5DgS5H1"}
;;      {:date-opened "2020-11-29T15:45:26.903345Z",
;;       :id "3316402",
;;       :account-holder "3498HO"}
;;      {:date-opened "2020-11-29T15:45:26.722964Z",
;;       :id "7235070",
;;       :account-holder "XW7RgZ"})}

  )
