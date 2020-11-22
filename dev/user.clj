(ns user)

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

  (defn uuid []
    (str (UUID/randomUUID)))
;; => #'user/uuid

  (defn execute-credit-transfer [debit-account credit-account amount]
    (let [tx-query "INSERT INTO transactions (id, account_id, amount) VALUES (?, ?, ?)"
          acct-query "UPDATE accounts SET balance = balance + ? WHERE id = ?"
          debit-amount (* amount -1)
          credit-amount amount]
      (jdbc/execute-one! db [tx-query (uuid) debit-account debit-amount])
      (jdbc/execute-one! db [tx-query (uuid) credit-account credit-amount])
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

  (def ashley-id
    (->> (jdbc/execute-one! db ["SELECT id FROM accounts WHERE account_holder = 'Ashley'"])
         :ACCOUNTS/ID))
;; => #'user/ashley-id

  (def kim-id
    (->> (jdbc/execute-one! db ["SELECT id FROM accounts WHERE account_holder = 'Kim'"])
         :ACCOUNTS/ID))
;; => #'user/kim-id  

  ashley-id
;; => "3688295"

  kim-id
;; => "7311890"

  (execute-credit-transfer ashley-id kim-id 42)
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

  (s/def :account/id (s/and string? #(= 7 (count %))))
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

  (s/def :account/id #{ashley-id kim-id})
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


  )
