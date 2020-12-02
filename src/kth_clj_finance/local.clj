(ns kth-clj-finance.local)

;; Break out your mainframe and let's get to banking!
(comment

  (require '[next.jdbc :as jdbc])
;; => nil

  (def db (jdbc/get-datasource {:dbtype "h2:mem" :dbname "finance"}))
;; => #'user/db

  (jdbc/execute-one! db ["
CREATE TABLE accounts (
  id CHAR(7) PRIMARY KEY,
  account_holder VARCHAR(255),
  balance INT DEFAULT 0,
  `date_opened` TIMESTAMP DEFAULT NOW(),
)"])
;; => #:next.jdbc{:update-count 0}

  (jdbc/execute-one! db ["
CREATE TABLE transactions (
  id VARCHAR(36) PRIMARY KEY,
  account_id CHAR(7),
  amount INT,
  `date` TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (account_id) REFERENCES accounts(id),
)"])
;; => #:next.jdbc{:update-count 0}

  (jdbc/execute-one! db ["SHOW TABLES"])
;; => #:TABLES{:TABLE_NAME "ACCOUNTS", :TABLE_SCHEMA "PUBLIC"}

  (jdbc/execute-one! db ["SHOW COLUMNS FROM accounts"])
;; => {:COLUMNS/FIELD "ID",
;;     :TYPE "CHAR(7)",
;;     :COLUMNS/NULL "NO",
;;     :KEY "PRI",
;;     :DEFAULT "NULL"}

  (jdbc/execute! db ["SHOW COLUMNS FROM accounts"])
;; => [{:COLUMNS/FIELD "ID",
;;      :TYPE "CHAR(7)",
;;      :COLUMNS/NULL "NO",
;;      :KEY "PRI",
;;      :DEFAULT "NULL"}
;;     {:COLUMNS/FIELD "ACCOUNT_HOLDER",
;;      :TYPE "VARCHAR(255)",
;;      :COLUMNS/NULL "YES",
;;      :KEY "",
;;      :DEFAULT "NULL"}
;;     {:COLUMNS/FIELD "BALANCE",
;;      :TYPE "INTEGER(10)",
;;      :COLUMNS/NULL "YES",
;;      :KEY "",
;;      :DEFAULT "0"}
;;     {:COLUMNS/FIELD "DATE_OPENED",
;;      :TYPE "TIMESTAMP(26)",
;;      :COLUMNS/NULL "YES",
;;      :KEY "",
;;      :DEFAULT "NOW()"}]

  (rand-int 9999999)
;; => 1836666;; => 19  

  (format "%07d" (rand-int 9999999))
;; => "3944352";; => "6004476";; => "6239291"  

  (format "%07d" (rand-int 999999))
;; => "0636545"

  (defn new-account-number []
    (format "%07d" (rand-int 9999999)))
;; => #'user/new-account-number

  (jdbc/execute-one! db ["
INSERT INTO accounts (id, account_holder) VALUES (?, ?)
" (new-account-number) "Ashley"])
;; => #:next.jdbc{:update-count 1}

  (jdbc/execute-one! db ["
INSERT INTO accounts (id, account_holder) VALUES (?, ?)
" (new-account-number) "Kim"])
;; => #:next.jdbc{:update-count 1}  

  (jdbc/execute! db ["SELECT * FROM accounts"])
;; => [#:ACCOUNTS{:ID "3688295",
;;                :ACCOUNT_HOLDER "Ashley",
;;                :BALANCE 0,
;;                :DATE_OPENED #inst "2020-11-22T14:54:23.073949000-00:00"}
;;     #:ACCOUNTS{:ID "7311890",
;;                :ACCOUNT_HOLDER "Kim",
;;                :BALANCE 0,
;;                :DATE_OPENED #inst "2020-11-22T14:54:41.043871000-00:00"}]

  (import '(java.util UUID))
;; => java.util.UUID

  (defn tx-id []
    (str (UUID/randomUUID)))
;; => #'user/tx-id

  (defn execute-credit-transfer [debit-account credit-account amount]
    (let [tx-query "INSERT INTO transactions (id, account_id, amount) VALUES (?, ?, ?)"
          acct-query "UPDATE accounts SET balance = balance + ? WHERE id = ?"
          debit-amount (* amount -1)
          credit-amount amount]
      (jdbc/execute-one! db [tx-query (tx-id) debit-account debit-amount])
      (jdbc/execute-one! db [tx-query (tx-id) credit-account credit-amount])
      (jdbc/execute-one! db [acct-query debit-amount debit-account])
      (jdbc/execute-one! db [acct-query credit-amount credit-account])))
;; => #'user/execute-credit-transfer

  (jdbc/execute-one! db ["DELETE FROM transactions"])
;; => #:next.jdbc{:update-count 1}

  (jdbc/execute-one! db ["SELECT id FROM accounts WHERE account_holder = 'Ashley'"])
;; => #:ACCOUNTS{:ID "3688295"}  

  (->> (jdbc/execute-one! db ["SELECT id FROM accounts WHERE account_holder = 'Ashley'"])
       :ACCOUNTS/ID)
;; => "3688295"  

  (defn account-id [holder]
    (->> (jdbc/execute-one! db ["SELECT id FROM accounts WHERE account_holder = ?" holder])
         :ACCOUNTS/ID))
;; => #'kth-clj-finance.local/account-id

  (account-id "Ashley")
;; => "3688295"

  (account-id "Kim")
;; => "7311890"

  (execute-credit-transfer (account-id "Ashley") (account-id "Kim") 42)
;; => #:next.jdbc{:update-count 1}

  (defn execute-one! [q]
    (let [qs (if (vector? q) q [q])]
      (jdbc/execute-one! db qs)))
;; => #'user/execute-one!

  (execute-one! "SELECT * FROM accounts")
;; => #:ACCOUNTS{:ID "6412469",
;;               :ACCOUNT_HOLDER "Ashley",
;;               :BALANCE -42,
;;               :DATE_OPENED #inst "2020-11-22T15:39:07.122688000-00:00"}

  (defn execute! [q]
    (let [qs (if (vector? q) q [q])]
      (jdbc/execute! db qs)))
;; => #'user/execute!

  (execute! "SELECT * FROM accounts")
;; => [#:ACCOUNTS{:ID "6412469",
;;                :ACCOUNT_HOLDER "Ashley",
;;                :BALANCE -42,
;;                :DATE_OPENED #inst "2020-11-22T15:39:07.122688000-00:00"}
;;     #:ACCOUNTS{:ID "2983099",
;;                :ACCOUNT_HOLDER "Kim",
;;                :BALANCE 42,
;;                :DATE_OPENED #inst "2020-11-22T15:39:10.304090000-00:00"}]

  (require '[clojure.spec.alpha :as s])
;; => nil

  (s/def :account/id (s/and string?
                            #(= 7 (count %))
                            (partial re-matches #"[0-9]+")))
;; => :account/id

  (s/conform :account/id 1)
;; => :clojure.spec.alpha/invalid

  (s/explain-data :account/id 1)
;; => #:clojure.spec.alpha{:problems
;;                         [{:path [],
;;                           :pred clojure.core/string?,
;;                           :val 1,
;;                           :via [:account/id],
;;                           :in []}],
;;                         :spec :account/id,
;;                         :value 1}

  (s/conform :account/id (new-account-number))
;; => "4782764"

  (s/fdef execute-credit-transfer
    :args (s/cat :debit-account :account/id
                 :credit-account :account/id
                 :amount pos-int?)
    :ret map?)
;; => user/execute-credit-transfer

  (s/exercise-fn `execute-credit-transfer)

  (require '[clojure.spec.gen.alpha :as sgen])
;; => nil

  (sgen/generate (s/gen :account/id))

  (sgen/generate (s/gen string?))
;; => "zag4N18t2o4BbL6"

  (count *1)
;; => 15

  (->> (repeatedly #(sgen/generate (s/gen string?)))
       (take 10)
       (map (juxt identity count)))
;; => (["FPGLkf7f2WvD3wi61x1l4" 21]
;;     ["miYC9tsk95432P32fX" 18]
;;     ["a0n418vzrp7Wq4B6J" 17]
;;     ["OdeX1FZai" 9]
;;     ["778Tf6r56n28ncyovG" 18]
;;     ["4qBBmG5VGj" 10]
;;     ["p791d9Q7Z4ZV0H780Yk32kTb" 24]
;;     ["FTx" 3]
;;     ["p91dPTMN29zg262M2TJJ85M0n" 25]
;;     ["20t93QIrrsh" 11])

  (s/def :account/id #{(account-id "Ashley") (account-id "Kim")})
;; => :account/id

  (s/exercise-fn `execute-credit-transfer)
;; => ([("2983099" "6412469" 1) #:next.jdbc{:update-count 1}]
;;     [("6412469" "6412469" 2) #:next.jdbc{:update-count 1}]
;;     [("2983099" "2983099" 2) #:next.jdbc{:update-count 1}]
;;     [("6412469" "6412469" 3) #:next.jdbc{:update-count 1}]
;;     [("6412469" "6412469" 1) #:next.jdbc{:update-count 1}]
;;     [("6412469" "6412469" 2) #:next.jdbc{:update-count 1}]
;;     [("6412469" "6412469" 3) #:next.jdbc{:update-count 1}]
;;     [("2983099" "2983099" 2) #:next.jdbc{:update-count 1}]
;;     [("6412469" "2983099" 2) #:next.jdbc{:update-count 1}]
;;     [("6412469" "2983099" 5) #:next.jdbc{:update-count 1}])

  (execute! "SELECT * FROM accounts")
;; => [#:ACCOUNTS{:ID "6412469",
;;                :ACCOUNT_HOLDER "Ashley",
;;                :BALANCE -48,
;;                :DATE_OPENED #inst "2020-11-22T15:39:07.122688000-00:00"}
;;     #:ACCOUNTS{:ID "2983099",
;;                :ACCOUNT_HOLDER "Kim",
;;                :BALANCE 48,
;;                :DATE_OPENED #inst "2020-11-22T15:39:10.304090000-00:00"}]

  (defn list-accounts []
    (execute! "SELECT * FROM accounts"))
;; => #'user/list-accounts

  (list-accounts)
;; => [#:ACCOUNTS{:ID "6412469",
;;                :ACCOUNT_HOLDER "Ashley",
;;                :BALANCE -48,
;;                :DATE_OPENED #inst "2020-11-22T15:39:07.122688000-00:00"}
;;     #:ACCOUNTS{:ID "2983099",
;;                :ACCOUNT_HOLDER "Kim",
;;                :BALANCE 48,
;;                :DATE_OPENED #inst "2020-11-22T15:39:10.304090000-00:00"}]

  (defn list-transactions [account-number]
    (execute! ["SELECT * FROM transactions WHERE account_id = ? ORDER BY `date` DESC"
               account-number]))
;; => #'user/list-transactions

  (list-transactions (account-id "Ashley"))
;; => [#:TRANSACTIONS{:ID "93292511-1fc1-4e6a-99e9-797a41b30a57",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT -5,
;;                    :DATE #inst "2020-11-22T15:48:53.925361000-00:00"}
;;     #:TRANSACTIONS{:ID "bc8fc12a-c8f5-4a2f-a8f4-84ee272a135c",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT -2,
;;                    :DATE #inst "2020-11-22T15:48:53.907486000-00:00"}
;;     #:TRANSACTIONS{:ID "406cfdba-ecbe-413c-a337-f92fe298273a",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT 3,
;;                    :DATE #inst "2020-11-22T15:48:53.889569000-00:00"}
;;     #:TRANSACTIONS{:ID "ae57f920-d478-4ef5-bf8a-a39020469412",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT -3,
;;                    :DATE #inst "2020-11-22T15:48:53.888422000-00:00"}
;;     #:TRANSACTIONS{:ID "2621bb94-e152-4ab0-a484-707af6d16d05",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT 2,
;;                    :DATE #inst "2020-11-22T15:48:53.878485000-00:00"}
;;     #:TRANSACTIONS{:ID "9d8319b8-f673-4980-8861-27b5de149012",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT -2,
;;                    :DATE #inst "2020-11-22T15:48:53.876334000-00:00"}
;;     #:TRANSACTIONS{:ID "93697e03-0f41-4e3a-82c6-aa04fe29e303",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT 1,
;;                    :DATE #inst "2020-11-22T15:48:53.867708000-00:00"}
;;     #:TRANSACTIONS{:ID "47b61e3f-0ae6-4cd6-880e-c9083059065b",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT -1,
;;                    :DATE #inst "2020-11-22T15:48:53.865700000-00:00"}
;;     #:TRANSACTIONS{:ID "fc9b62a6-ce29-4bdc-8525-2f5ac0b2fdc3",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT 3,
;;                    :DATE #inst "2020-11-22T15:48:53.855628000-00:00"}
;;     #:TRANSACTIONS{:ID "b3ea48d9-2a37-4ad1-b27f-40aeac3c5266",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT -3,
;;                    :DATE #inst "2020-11-22T15:48:53.854312000-00:00"}
;;     #:TRANSACTIONS{:ID "a5361e68-4801-4be2-a5a4-0dc5fddfe52d",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT 2,
;;                    :DATE #inst "2020-11-22T15:48:53.817829000-00:00"}
;;     #:TRANSACTIONS{:ID "dd419a87-550c-4d58-9322-d75520981b1d",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT -2,
;;                    :DATE #inst "2020-11-22T15:48:53.813769000-00:00"}
;;     #:TRANSACTIONS{:ID "4d4d7330-f111-40c8-b9bd-61716f31cf82",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT 1,
;;                    :DATE #inst "2020-11-22T15:48:53.777892000-00:00"}
;;     #:TRANSACTIONS{:ID "70bbb213-57a5-4ca3-ad27-e207a90f8e63",
;;                    :ACCOUNT_ID "6412469",
;;                    :AMOUNT -42,
;;                    :DATE #inst "2020-11-22T15:43:23.463966000-00:00"}]

  (defn get-balance [account-number]
    (->> (execute-one! ["SELECT balance FROM accounts WHERE id = ?"
                        account-number])
         :ACCOUNTS/BALANCE))
;; => #'user/get-balance

  (get-balance (account-id "Ashley"))
;; => -48

  (defn get-balance
    ([account-number]
    (->> (execute-one! ["SELECT balance FROM accounts WHERE id = ?"
                        account-number])
         :ACCOUNTS/BALANCE))

    ([account-number as-of-date]
     :???))

  ;; Also, what if another transfer happens at the same time?

  )

;; The functional way!
(comment

  (def accounts (atom {}))
;; => #'user/accounts

  (s/def :account/account-holder string?)
;; => :account/account-holder

  (s/def :account/date-opened (s/and string?
                                     (partial re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z")))
;; => :account/date-opened

  (s/def :account/account (s/keys :req-un [:account/id
                                           :account/account-holder
                                           :account/date-opened]))
;; => :account/account

  (import '(java.time Instant))
;; => java.time.Instant  

  (str (Instant/now))
;; => "2020-11-22T16:08:47.240299Z"

  (defn now []
    (str (Instant/now)))
;; => #'user/now  

  (defn create-account [account-holder]
    (let [account {:id (new-account-number)
                   :account-holder account-holder
                   :date-opened (now)}]
      (swap! accounts assoc (:id account) account)))
;; => #'user/create-account

  (create-account "Ashley")
;; => {"8380288"
;;     {:id "8380288",
;;      :account-holder "Ashley",
;;      :date-opened "2020-11-22T16:09:49.737288Z"}}

  (->> (vals *1)
       first
       (s/valid? :account/account))
;; => false

  (->> (vals @accounts)
       first
       (s/explain-data :account/account))
;; => #:clojure.spec.alpha{:problems
;;                         ({:path [:id],
;;                           :pred #{(account-id "Ashley") (account-id "Kim")},
;;                           :val "9459451",
;;                           :via [:account/account :account/id],
;;                           :in [:id]}),
;;                         :spec :account/account,
;;                         :value
;;                         {:id "9459451",
;;                          :account-holder "Ashley",
;;                          :date-opened "2020-12-01T07:34:19.207358Z"}}

  (s/def :account/id (s/and string?
                            #(= 7 (count %))
                            (partial re-matches #"[0-9]+")))
;; => :account/id

  (->> (vals @accounts)
       first
       (s/valid? :account/account))
;; => true

  (create-account "Kim")
;; => {"8380288"
;;     {:id "8380288",
;;      :account-holder "Ashley",
;;      :date-opened "2020-11-22T16:09:49.737288Z"},
;;     "6489301"
;;     {:id "6489301",
;;      :account-holder "Kim",
;;      :date-opened "2020-11-22T16:10:04.948452Z"}}  

  (defn account-id [account-holder]
    (->> @accounts
         (some (fn [[id account]]
                 (= account-holder (:account-holder account))))))
;; => #'user/account-id

  (account-id "Kim")
;; => true

  (defn account-id [account-holder]
    (->> @accounts
         (some (fn [[id account]]
                 (and (= account-holder (:account-holder account))
                      id)))))
;; => #'user/account-id

  (account-id "Kim")
;; => "6489301"

  (def ledger (atom []))
;; => #'user/ledger

  (defn execute-credit-transfer [debit-account credit-account amount]
    (let [tx {:id (tx-id)
              :debit-account debit-account
              :credit-account credit-account
              :amount amount
              :date (now)}]
      (swap! ledger conj tx)
      tx))
;; => #'user/execute-credit-transfer

  (execute-credit-transfer (account-id "Ashley") (account-id "Kim") 42)
;; => [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;      :debit-account "8380288",
;;      :credit-account "6489301",
;;      :amount 42,
;;      :date "2020-11-22T16:16:58.172085Z"}]

  (defn list-transactions [account-number]
    (->> @ledger
         (filter (fn [{:keys [debit-account credit-account] :as tx}]
                   (or (= account-number debit-account)
                       (= account-number credit-account))))))
;; => #'user/list-transactions

  (list-transactions (account-id "Ashley"))
;; => ({:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;      :debit-account "8380288",
;;      :credit-account "6489301",
;;      :amount 42,
;;      :date "2020-11-22T16:16:58.172085Z"})

  (defn list-transactions [account-number]
    (->> @ledger
         (filter (fn [{:keys [debit-account credit-account] :as tx}]
                   (or (= account-number debit-account)
                       (= account-number credit-account))))
         (map (fn [{:keys [debit-account credit-account amount] :as tx}]
                (-> (select-keys tx [:id :date])
                    (assoc :amount (if (= account-number debit-account)
                                     (* amount -1)
                                     amount)))))))
;; => #'user/list-transactions

  (list-transactions (account-id "Ashley"))
;; => ({:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;      :date "2020-11-22T16:16:58.172085Z",
;;      :amount -42})

  (s/def :account/id #{(account-id "Ashley") (account-id "Kim")})
;; => :account/id

  (s/exercise-fn `execute-credit-transfer)
;; => ([("6489301" "6489301" 2)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}]]
;;     [("8380288" "6489301" 1)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}]]
;;     [("8380288" "6489301" 1)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}]]
;;     [("6489301" "8380288" 4)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}]]
;;     [("6489301" "8380288" 2)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}]]
;;     [("8380288" "8380288" 5)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}]]
;;     [("8380288" "8380288" 2)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}]]
;;     [("6489301" "6489301" 6)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}]]
;;     [("8380288" "8380288" 1)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}]]
;;     [("6489301" "6489301" 74)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}]])

  (list-transactions (account-id "Ashley"))
;; => ({:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;      :date "2020-11-22T16:16:58.172085Z",
;;      :amount -42}
;;     {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;      :date "2020-11-22T16:26:14.113794Z",
;;      :amount -1}
;;     {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;      :date "2020-11-22T16:26:14.161013Z",
;;      :amount -1}
;;     {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;      :date "2020-11-22T16:26:14.187429Z",
;;      :amount 4}
;;     {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;      :date "2020-11-22T16:26:14.224974Z",
;;      :amount 2}
;;     {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;      :date "2020-11-22T16:26:14.257782Z",
;;      :amount -5}
;;     {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;      :date "2020-11-22T16:26:14.304439Z",
;;      :amount -2}
;;     {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;      :date "2020-11-22T16:26:14.380766Z",
;;      :amount -1})  

  (create-account "Sam")
;; => {"7295776" "Ashley",
;;     "8380288"
;;     {:id "8380288",
;;      :account-holder "Ashley",
;;      :date-opened "2020-11-22T16:09:49.737288Z"},
;;     "6489301"
;;     {:id "6489301",
;;      :account-holder "Kim",
;;      :date-opened "2020-11-22T16:10:04.948452Z"},
;;     "3250090"
;;     {:id "3250090",
;;      :account-holder "Sam",
;;      :date-opened "2020-11-22T16:27:44.661525Z"}}

  (account-id "Sam")
;; => "4279525"

  (s/def :account/id #{(account-id "Ashley") (account-id "Kim") (account-id "Sam")})
;; => :account/id  

  (s/exercise-fn `execute-credit-transfer)
;; => ([("8380288" "8380288" 2)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}]]
;;     [("8380288" "6489301" 1)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}
;;       {:id "3822520c-718f-404a-b8bb-c6d9601ab2c4",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.369507Z"}]]
;;     [("6489301" "3250090" 2)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}
;;       {:id "3822520c-718f-404a-b8bb-c6d9601ab2c4",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.369507Z"}
;;       {:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.431042Z"}]]
;;     [("6489301" "3250090" 1)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}
;;       {:id "3822520c-718f-404a-b8bb-c6d9601ab2c4",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.369507Z"}
;;       {:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.431042Z"}
;;       {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.502501Z"}]]
;;     [("3250090" "8380288" 2)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}
;;       {:id "3822520c-718f-404a-b8bb-c6d9601ab2c4",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.369507Z"}
;;       {:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.431042Z"}
;;       {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.502501Z"}
;;       {:id "d5eadbb6-2080-4c83-b3a1-1b970f49006c",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.563273Z"}]]
;;     [("6489301" "3250090" 3)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}
;;       {:id "3822520c-718f-404a-b8bb-c6d9601ab2c4",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.369507Z"}
;;       {:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.431042Z"}
;;       {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.502501Z"}
;;       {:id "d5eadbb6-2080-4c83-b3a1-1b970f49006c",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.563273Z"}
;;       {:id "2fc15eb3-cc7d-4cd5-a0a8-7737df15c08a",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 3,
;;        :date "2020-11-22T16:28:47.638465Z"}]]
;;     [("8380288" "8380288" 7)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}
;;       {:id "3822520c-718f-404a-b8bb-c6d9601ab2c4",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.369507Z"}
;;       {:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.431042Z"}
;;       {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.502501Z"}
;;       {:id "d5eadbb6-2080-4c83-b3a1-1b970f49006c",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.563273Z"}
;;       {:id "2fc15eb3-cc7d-4cd5-a0a8-7737df15c08a",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 3,
;;        :date "2020-11-22T16:28:47.638465Z"}
;;       {:id "9679086d-c347-4da5-9a3c-3795e06211f3",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 7,
;;        :date "2020-11-22T16:28:47.722154Z"}]]
;;     [("3250090" "6489301" 5)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}
;;       {:id "3822520c-718f-404a-b8bb-c6d9601ab2c4",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.369507Z"}
;;       {:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.431042Z"}
;;       {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.502501Z"}
;;       {:id "d5eadbb6-2080-4c83-b3a1-1b970f49006c",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.563273Z"}
;;       {:id "2fc15eb3-cc7d-4cd5-a0a8-7737df15c08a",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 3,
;;        :date "2020-11-22T16:28:47.638465Z"}
;;       {:id "9679086d-c347-4da5-9a3c-3795e06211f3",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 7,
;;        :date "2020-11-22T16:28:47.722154Z"}
;;       {:id "356ce0f2-ad53-4703-a514-e53d1610530c",
;;        :debit-account "3250090",
;;        :credit-account "6489301",
;;        :amount 5,
;;        :date "2020-11-22T16:28:47.779346Z"}]]
;;     [("8380288" "6489301" 2)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}
;;       {:id "3822520c-718f-404a-b8bb-c6d9601ab2c4",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.369507Z"}
;;       {:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.431042Z"}
;;       {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.502501Z"}
;;       {:id "d5eadbb6-2080-4c83-b3a1-1b970f49006c",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.563273Z"}
;;       {:id "2fc15eb3-cc7d-4cd5-a0a8-7737df15c08a",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 3,
;;        :date "2020-11-22T16:28:47.638465Z"}
;;       {:id "9679086d-c347-4da5-9a3c-3795e06211f3",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 7,
;;        :date "2020-11-22T16:28:47.722154Z"}
;;       {:id "356ce0f2-ad53-4703-a514-e53d1610530c",
;;        :debit-account "3250090",
;;        :credit-account "6489301",
;;        :amount 5,
;;        :date "2020-11-22T16:28:47.779346Z"}
;;       {:id "817d5ae6-1584-4358-945b-9028b57dbd84",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.889120Z"}]]
;;     [("3250090" "8380288" 10)
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}
;;       {:id "3822520c-718f-404a-b8bb-c6d9601ab2c4",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.369507Z"}
;;       {:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.431042Z"}
;;       {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.502501Z"}
;;       {:id "d5eadbb6-2080-4c83-b3a1-1b970f49006c",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.563273Z"}
;;       {:id "2fc15eb3-cc7d-4cd5-a0a8-7737df15c08a",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 3,
;;        :date "2020-11-22T16:28:47.638465Z"}
;;       {:id "9679086d-c347-4da5-9a3c-3795e06211f3",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 7,
;;        :date "2020-11-22T16:28:47.722154Z"}
;;       {:id "356ce0f2-ad53-4703-a514-e53d1610530c",
;;        :debit-account "3250090",
;;        :credit-account "6489301",
;;        :amount 5,
;;        :date "2020-11-22T16:28:47.779346Z"}
;;       {:id "817d5ae6-1584-4358-945b-9028b57dbd84",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.889120Z"}
;;       {:id "05ba7993-d376-4d51-a02a-6feedb13c4e3",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 10,
;;        :date "2020-11-22T16:28:47.961143Z"}]])  

  (list-transactions (account-id "Sam"))
;; => ({:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;      :date "2020-11-22T16:28:47.431042Z",
;;      :amount 2}
;;     {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;      :date "2020-11-22T16:28:47.502501Z",
;;      :amount 1}
;;     {:id "d5eadbb6-2080-4c83-b3a1-1b970f49006c",
;;      :date "2020-11-22T16:28:47.563273Z",
;;      :amount -2}
;;     {:id "2fc15eb3-cc7d-4cd5-a0a8-7737df15c08a",
;;      :date "2020-11-22T16:28:47.638465Z",
;;      :amount 3}
;;     {:id "356ce0f2-ad53-4703-a514-e53d1610530c",
;;      :date "2020-11-22T16:28:47.779346Z",
;;      :amount -5}
;;     {:id "05ba7993-d376-4d51-a02a-6feedb13c4e3",
;;      :date "2020-11-22T16:28:47.961143Z",
;;      :amount -10})

  (defn list-transactions [account-number]
    (let [xf (comp
              (filter (fn [{:keys [debit-account credit-account] :as tx}]
                        (or (= account-number debit-account)
                            (= account-number credit-account))))
              (map (fn [{:keys [debit-account credit-account amount] :as tx}]
                     (-> (select-keys tx [:id :date])
                         (assoc :amount (if (= account-number debit-account)
                                          (* amount -1)
                                          amount))))))]
      (transduce xf conj @ledger)))
;; => #'user/list-transactions  

  (list-transactions (account-id "Sam"))
;; => [{:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;      :date "2020-11-22T16:28:47.431042Z",
;;      :amount 2}
;;     {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;      :date "2020-11-22T16:28:47.502501Z",
;;      :amount 1}
;;     {:id "d5eadbb6-2080-4c83-b3a1-1b970f49006c",
;;      :date "2020-11-22T16:28:47.563273Z",
;;      :amount -2}
;;     {:id "2fc15eb3-cc7d-4cd5-a0a8-7737df15c08a",
;;      :date "2020-11-22T16:28:47.638465Z",
;;      :amount 3}
;;     {:id "356ce0f2-ad53-4703-a514-e53d1610530c",
;;      :date "2020-11-22T16:28:47.779346Z",
;;      :amount -5}
;;     {:id "05ba7993-d376-4d51-a02a-6feedb13c4e3",
;;      :date "2020-11-22T16:28:47.961143Z",
;;      :amount -10}]

  (defn transaction-lister [account-number]
    (comp
     (filter (fn [{:keys [debit-account credit-account] :as tx}]
               (or (= account-number debit-account)
                   (= account-number credit-account))))
     (map (fn [{:keys [debit-account credit-account amount] :as tx}]
            (-> (select-keys tx [:id :date])
                (assoc :amount (if (= account-number debit-account)
                                 (* amount -1)
                                 amount)))))))
;; => #'user/transaction-lister  

  (defn list-transactions [account-number]
    (transduce (transaction-lister account-number) conj @ledger))
;; => #'user/list-transactions  

  (list-transactions (account-id "Sam"))
;; => [{:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;      :date "2020-11-22T16:28:47.431042Z",
;;      :amount 2}
;;     {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;      :date "2020-11-22T16:28:47.502501Z",
;;      :amount 1}
;;     {:id "d5eadbb6-2080-4c83-b3a1-1b970f49006c",
;;      :date "2020-11-22T16:28:47.563273Z",
;;      :amount -2}
;;     {:id "2fc15eb3-cc7d-4cd5-a0a8-7737df15c08a",
;;      :date "2020-11-22T16:28:47.638465Z",
;;      :amount 3}
;;     {:id "356ce0f2-ad53-4703-a514-e53d1610530c",
;;      :date "2020-11-22T16:28:47.779346Z",
;;      :amount -5}
;;     {:id "05ba7993-d376-4d51-a02a-6feedb13c4e3",
;;      :date "2020-11-22T16:28:47.961143Z",
;;      :amount -10}]

  (defn get-balance
    ([account-number]
     (transduce (comp (transaction-lister account-number)
                      (map :amount))
                +
                @ledger)))
;; => #'user/get-balance

  (get-balance (account-id "Sam"))
;; => -11

  (Instant/parse (now))
;; => #object[java.time.Instant 0x29372373 "2020-11-22T16:55:13.877055Z"]  

  (.isBefore (Instant/parse "2020-11-22T16:55:13.877055Z") (Instant/parse (now)))
;; => true  

  (.compareTo (Instant/parse "2020-11-22T16:55:13.877055Z") (Instant/parse (now)))
;; => -1

  (let [now (Instant/now)]
    (.compareTo now now))
;; => 0  

  (defn get-balance
    ([account-number]
     (get-balance account-number (now)))

    ([account-number as-of-date]
     (let [as-of-instant (Instant/parse as-of-date)]
       (transduce (comp (transaction-lister account-number)
                        (filter (fn [{:keys [date]}]
                                  (<= (.compareTo (Instant/parse date) as-of-instant) 0)))
                        (map :amount))
                  +
                  @ledger))))
;; => #'user/get-balance

  (execute-credit-transfer (account-id "Ashley") (account-id "Sam") 42)
;; => {:id "fa2cfb1b-e633-4784-8599-3a832896c88a",
;;     :debit-account "8380288",
;;     :credit-account "3250090",
;;     :amount 42,
;;     :date "2020-11-22T17:03:32.299003Z"}

  (get-balance (account-id "Sam"))
;; => 115  

  (get-balance (account-id "Sam") "2020-11-22T17:00:00Z")
;; => -11  

  ;; Let's make balance lookups faster!

  (def balances (atom {}))
;; => #'user/balances

  (defn update-balances []
    (->> (keys @accounts)
         (map (fn [account-number]
                [account-number (get-balance account-number)]))
         (into {})
         (reset! balances)))
;; => #'user/update-balances  

  @balances
;; => {}

  (update-balances)
;; => {"7295776" 0, "8380288" -172, "6489301" -42, "3250090" 115}

  @balances
;; => {"7295776" 0, "8380288" -172, "6489301" -42, "3250090" 115}

  (defn get-cached-balance [account-number]
    (get @balances account-number))
;; => #'user/get-cached-balance

  (get-balance (account-id "Sam"))
;; => 115  

  (get-cached-balance (account-id "Sam"))
;; => 115

  (execute-credit-transfer (account-id "Sam") (account-id "Ashley") 5)
;; => {:id "d79b67a8-7e2b-4cb5-938c-68b8f94aea13",
;;     :debit-account "3250090",
;;     :credit-account "8380288",
;;     :amount 5,
;;     :date "2020-11-22T17:15:54.994817Z"}  

  (get-balance (account-id "Sam"))
;; => 110

  (get-cached-balance (account-id "Sam"))
;; => 115

  ((fnil - 0) 20)
;; => -20

  (add-watch ledger :balance-updater
             (fn [_key _atom _old_state new-state]
               (let [{:keys [debit-account credit-account amount]} (first (rseq new-state))]
                 (swap! balances update debit-account (fnil - 0) amount)
                 (swap! balances update credit-account (fnil + 0) amount))))
;; => #<Atom@2873f4b8: 
;;      [{:id "873ed4ab-cb45-438f-9ecc-e9105e1eda1c",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 42,
;;        :date "2020-11-22T16:16:58.172085Z"}
;;       {:id "696df266-a05b-4812-9a8e-0a7accc0b860",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.069905Z"}
;;       {:id "7b6100f3-f7d6-4f8e-9c11-6a462042cc9a",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.113794Z"}
;;       {:id "c9815bf4-bcd3-4fe3-9c65-256292f48d1e",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.161013Z"}
;;       {:id "9cd3a166-d550-4657-a14c-c9a4c90b26b4",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 4,
;;        :date "2020-11-22T16:26:14.187429Z"}
;;       {:id "8fd55a89-2bb7-4f21-b410-afc8ec816894",
;;        :debit-account "6489301",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.224974Z"}
;;       {:id "3ffd9b06-20b5-464f-b1b3-0208240545e6",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T16:26:14.257782Z"}
;;       {:id "40129d53-1f6d-4fe2-950c-95efe6d2b6cb",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:26:14.304439Z"}
;;       {:id "a704025b-8e06-4cd5-b0fa-e3fc1113c2f9",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 6,
;;        :date "2020-11-22T16:26:14.338984Z"}
;;       {:id "09bd2f7a-862a-470d-81f3-a86609110e8f",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 1,
;;        :date "2020-11-22T16:26:14.380766Z"}
;;       {:id "4137a788-e0d2-4888-85cd-e05fc1dd5573",
;;        :debit-account "6489301",
;;        :credit-account "6489301",
;;        :amount 74,
;;        :date "2020-11-22T16:26:14.437684Z"}
;;       {:id "dd9f5c32-ae02-431a-983d-512e0ffa5cbd",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.315407Z"}
;;       {:id "3822520c-718f-404a-b8bb-c6d9601ab2c4",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.369507Z"}
;;       {:id "a46d7f18-b9d2-44a8-8f97-b5de573937aa",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.431042Z"}
;;       {:id "5c141992-3d10-4cc7-af4a-6d8079f218bf",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 1,
;;        :date "2020-11-22T16:28:47.502501Z"}
;;       {:id "d5eadbb6-2080-4c83-b3a1-1b970f49006c",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.563273Z"}
;;       {:id "2fc15eb3-cc7d-4cd5-a0a8-7737df15c08a",
;;        :debit-account "6489301",
;;        :credit-account "3250090",
;;        :amount 3,
;;        :date "2020-11-22T16:28:47.638465Z"}
;;       {:id "9679086d-c347-4da5-9a3c-3795e06211f3",
;;        :debit-account "8380288",
;;        :credit-account "8380288",
;;        :amount 7,
;;        :date "2020-11-22T16:28:47.722154Z"}
;;       {:id "356ce0f2-ad53-4703-a514-e53d1610530c",
;;        :debit-account "3250090",
;;        :credit-account "6489301",
;;        :amount 5,
;;        :date "2020-11-22T16:28:47.779346Z"}
;;       {:id "817d5ae6-1584-4358-945b-9028b57dbd84",
;;        :debit-account "8380288",
;;        :credit-account "6489301",
;;        :amount 2,
;;        :date "2020-11-22T16:28:47.889120Z"}
;;       {:id "05ba7993-d376-4d51-a02a-6feedb13c4e3",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 10,
;;        :date "2020-11-22T16:28:47.961143Z"}
;;       {:id "ecf51c35-e8be-4da0-aba8-0355cdf21162",
;;        :debit-account "8380288",
;;        :credit-account "3250090",
;;        :amount 42,
;;        :date "2020-11-22T17:00:15.294496Z"}
;;       {:id "85118b42-578b-491d-8981-71ff5bee5d4f",
;;        :debit-account "8380288",
;;        :credit-account "3250090",
;;        :amount 42,
;;        :date "2020-11-22T17:03:04.865871Z"}
;;       {:id "fa2cfb1b-e633-4784-8599-3a832896c88a",
;;        :debit-account "8380288",
;;        :credit-account "3250090",
;;        :amount 42,
;;        :date "2020-11-22T17:03:32.299003Z"}
;;       {:id "d79b67a8-7e2b-4cb5-938c-68b8f94aea13",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 5,
;;        :date "2020-11-22T17:15:54.994817Z"}
;;       {:id "7676f431-3ad8-45d9-b289-76dbddeb655f",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 10,
;;        :date "2020-11-22T17:20:50.780079Z"}
;;       {:id "905e39a8-0807-4cf6-b0fc-b795a35b288d",
;;        :debit-account "3250090",
;;        :credit-account "8380288",
;;        :amount 10,
;;        :date "2020-11-22T17:21:01.648507Z"}]>

  (update-balances)
;; => {"7295776" 0, "8380288" -147, "6489301" -42, "3250090" 90}  

  (get-balance (account-id "Sam"))
;; => 90

  (get-cached-balance (account-id "Sam"))
;; => 90

  (execute-credit-transfer (account-id "Sam") (account-id "Ashley") 45)  
;; => {:id "b26f8b3a-740f-4a18-9ab6-170bb9434cc6",
;;     :debit-account "3250090",
;;     :credit-account "8380288",
;;     :amount 45,
;;     :date "2020-11-22T17:25:24.098911Z"}


  (get-balance (account-id "Sam"))
;; => 45

  (get-cached-balance (account-id "Sam"))
;; => 45

  )
