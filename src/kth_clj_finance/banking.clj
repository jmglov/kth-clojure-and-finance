(ns kth-clj-finance.banking)

;; Break out your mainframe and let's get to banking!
(comment
















  ;; Database tables
  ;;
  ;;                            +--------------+
  ;; +----------------+         | transactions |
  ;; | accounts       |         +--------------+
  ;; +----------------+         | id           |
  ;; | id             |---------| account_id   |
  ;; | account_holder |         | amount       |
  ;; | balance        |         | date         |
  ;; | date_opened    |         +--------------+
  ;; +----------------+
















  (require '[next.jdbc :as jdbc])

















  (def db (jdbc/get-datasource {:dbtype "h2:mem" :dbname "finance"}))


















  (jdbc/execute-one! db ["
CREATE TABLE accounts (
  id CHAR(7) PRIMARY KEY,
  account_holder VARCHAR(255),
  balance INT DEFAULT 0,
  `date_opened` TIMESTAMP DEFAULT NOW(),
)"])

















  (jdbc/execute-one! db ["
CREATE TABLE transactions (
  id VARCHAR(36) PRIMARY KEY,
  account_id CHAR(7),
  amount INT,
  `date` TIMESTAMP DEFAULT NOW(),
  FOREIGN KEY (account_id) REFERENCES accounts(id),
)"])


















  (jdbc/execute-one! db ["SHOW TABLES"])

















  (jdbc/execute-one! db ["SHOW COLUMNS FROM accounts"])


















  (jdbc/execute! db ["SHOW COLUMNS FROM accounts"])


















  ;; - A Swedish account number is 7 digits (usually)
  ;; - It must be unique per bank
  ;; - How can we generate one?

















  ;; How about a random integer?
  ;; Unlikely to collide for the lifetime of our bank,
  ;; which is only this REPL session. ;)


















  (rand-int 9999999)






















  ;; But it needs to be a string in our DB

















  (format "%07d" (rand-int 9999999))






















  ;; Test our zero padding

















  (format "%07d" (rand-int 10))


















  (defn new-account-number []
    (format "%07d" (rand-int 9999999)))



















  ;; Let's open some accounts!



















  (jdbc/execute-one! db ["
INSERT INTO accounts (id, account_holder) VALUES (?, ?)
" (new-account-number) "Ashley"])


















  (jdbc/execute-one! db ["
INSERT INTO accounts (id, account_holder) VALUES (?, ?)
" (new-account-number) "Kim"])


















  (jdbc/execute! db ["SELECT * FROM accounts"])


















  ;; Now, let's move some money!


















  ;; +------------------+
  ;; | transactions     |
  ;; +------------------+
  ;; | id           str |
  ;; | account_id   str |
  ;; | amount       int |
  ;; | date         ts  |
  ;; +------------------+

  ;; - account_id references an account
  ;; - amount is in minor units (öre / cents / yen)
  ;; - date is date
  ;; - id is ???


















  ;; - must be unique (it's a primary key)
  ;; - otherwise not meaningful

















  (import '(java.util UUID))


















  (defn tx-id []
    (str (UUID/randomUUID)))



















  ;; Credit transfer: move money from one account to another
  ;; 1. Debit source account
  ;; 2. Credit destination account
  ;; 3. Update source account balance
  ;; 4. Update destination account balance


















  (defn execute-credit-transfer [debit-account credit-account amount]
    (let [tx-query "INSERT INTO transactions (id, account_id, amount) VALUES (?, ?, ?)"
          acct-query "UPDATE accounts SET balance = balance + ? WHERE id = ?"
          debit-amount (* amount -1)
          credit-amount amount]
      (jdbc/execute-one! db [tx-query (tx-id) debit-account debit-amount])
      (jdbc/execute-one! db [tx-query (tx-id) credit-account credit-amount])
      (jdbc/execute-one! db [acct-query debit-amount debit-account])
      (jdbc/execute-one! db [acct-query credit-amount credit-account])))



















  ;; How to look up an account ID?

















  (jdbc/execute-one! db ["SELECT id FROM accounts WHERE account_holder = 'Ashley'"])


















  (->> (jdbc/execute-one! db ["SELECT id FROM accounts WHERE account_holder = 'Ashley'"])
       :ACCOUNTS/ID)


















  (defn account-id [holder]
    (->> (jdbc/execute-one! db ["SELECT id FROM accounts WHERE account_holder = ?" holder])
         :ACCOUNTS/ID))


















  (account-id "Ashley")


















  (account-id "Kim")


















  (execute-credit-transfer (account-id "Ashley") (account-id "Kim") 42)

















  ;; Type less, smile more!
  ;; Execute database queries more easily

















  (defn execute-one! [q]
    (let [qs (if (vector? q) q [q])]
      (jdbc/execute-one! db qs)))

















  (execute-one! "SELECT * FROM accounts")


















  ;; OK, cool...
  ;; but where are the rest of my accounts

















  (defn execute! [q]
    (let [qs (if (vector? q) q [q])]
      (jdbc/execute! db qs)))


















  (execute! "SELECT * FROM accounts")


















  ;; Moving money without a type system?
  ;; I'm scared!!!

















  (require '[clojure.spec.alpha :as s])


















  (s/def :account/id (s/and string?
                            #(= 7 (count %))
                            (partial re-matches #"[0-9]+")))


















  (s/conform :account/id 1)


















  (s/explain-data :account/id 1)


















  (s/conform :account/id (new-account-number))


















  ;; What can we say about a credit transfer?


















  (s/fdef execute-credit-transfer
    :args (s/cat :debit-account :account/id
                 :credit-account :account/id
                 :amount pos-int?)
    :ret map?)


















  ;; So let's ask spec to move some money!


















  (s/exercise-fn `execute-credit-transfer)



















  ;; What went wrong?

















  ;; s/exercise-fn generates params for the function;
  ;; let's try generating some of our own


















  (require '[clojure.spec.gen.alpha :as sgen])

















  (sgen/generate (s/gen :account/id))

















  (sgen/generate (s/gen string?))



















  (count *1)




















  (->> (repeatedly #(sgen/generate (s/gen string?)))
       (take 10)
       (map (juxt identity count)))


















  ;; What do we do when we encounter a hard problem?


















  ;; Cheat!

  (s/def :account/id #{(account-id "Ashley") (account-id "Kim")})



















  (sgen/generate (s/gen :account/id))


















  ;; OK, let's try to move some money again!


















  (s/exercise-fn `execute-credit-transfer)


















  (execute! "SELECT * FROM accounts")


















  (defn list-accounts []
    (execute! "SELECT * FROM accounts"))


















  (list-accounts)


















  ;; Where did my money go!?


















  (defn list-transactions [account-number]
    (execute! ["SELECT * FROM transactions WHERE account_id = ? ORDER BY `date` DESC"
               account-number]))


















  (list-transactions (account-id "Ashley"))



















  ;; And how much money do I have now?






















  (defn get-balance [account-number]
    (->> (execute-one! ["SELECT balance FROM accounts WHERE id = ?"
                        account-number])
         :ACCOUNTS/BALANCE))



















  (get-balance (account-id "Ashley"))


















  ;; Cool! How much money did I have last Thursday?


















  (defn get-balance
    ([account-number]
    (->> (execute-one! ["SELECT balance FROM accounts WHERE id = ?"
                        account-number])
         :ACCOUNTS/BALANCE))

    ([account-number as-of-date]
     :???))


















  
  ;; Um... let me get back to you?


















  ;; And what happens if two transfers are
  ;; executed at about the same time?

  )


















;; Let's get functional!

(comment



















  ;; We need a place to keep our accounts

















  
  (def accounts (atom {}))


















  ;; Now what goes in there?


















  (s/def :account/account-holder string?)


  (s/def :account/date-opened (s/and string?
                                     (partial re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z")))


  (s/def :account/account (s/keys :req-un [:account/id
                                           :account/account-holder
                                           :account/date-opened]))




















  ;; Let's make a date



















  (import '(java.time Instant))



















  (str (Instant/now))



















  (defn now []
    (str (Instant/now)))




















  (defn create-account [account-holder]
    (let [account {:id (new-account-number)
                   :account-holder account-holder
                   :date-opened (now)}]
      (swap! accounts assoc (:id account) account)))


















  (create-account "Ashley")


















  (->> (vals *1)
       first
       (s/valid? :account/account))



















  ;; Wait, what?


















  (->> (vals @accounts)
       first
       (s/explain-data :account/account))



















  ;; Oops...


















  (s/def :account/id (s/and string?
                            #(= 7 (count %))
                            (partial re-matches #"[0-9]+")))


















  (->> (vals @accounts)
       first
       (s/valid? :account/account))


















  (create-account "Kim")




















  ;; What's my account number?


















  (defn account-id [account-holder]
    (->> @accounts
         (some (fn [[id account]]
                 (= account-holder (:account-holder account))))))



















  (account-id "Kim")


















  ;; Ugh, someone failed the Turing test!


















  (defn account-id [account-holder]
    (->> @accounts
         (some (fn [[id account]]
                 (and (= account-holder (:account-holder account))
                      id)))))



















  (account-id "Kim")



















  ;; Now I need somewhere to put my money



















  (def ledger (atom []))



















  ;; And some way to move it



















  (defn execute-credit-transfer [debit-account credit-account amount]
    (let [tx {:id (tx-id)
              :debit-account debit-account
              :credit-account credit-account
              :amount amount
              :date (now)}]
      (swap! ledger conj tx)
      tx))

















  (execute-credit-transfer (account-id "Ashley")
                           (account-id "Kim")
                           42)


















  ;; So what just happened?



















  (defn list-transactions [account-number]
    (->> @ledger
         (filter (fn [{:keys [debit-account credit-account] :as tx}]
                   (or (= account-number debit-account)
                       (= account-number credit-account))))))


















  (list-transactions (account-id "Ashley"))



















  ;; Wait, didn't I send money?
  ;; Why so positive?


















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


















  (list-transactions (account-id "Ashley"))


















  ;; Yeah, makes more sense



















  ;; Let's move some more money around!


















  (s/def :account/id #{(account-id "Ashley") (account-id "Kim")})



















  (s/exercise-fn `execute-credit-transfer)



















  (list-transactions (account-id "Ashley"))


















  ;; Transfer ping pong gets boring after a while :()


















  (create-account "Sam")


















  (account-id "Sam")


















  (s/def :account/id #{(account-id "Ashley")
                       (account-id "Kim")
                       (account-id "Sam")})



















  (s/exercise-fn `execute-credit-transfer)



















  (list-transactions (account-id "Sam"))



















  ;; Now that we have a lot (*ahem*) of transactions,
  ;; we suddenly care about efficiency



















  ;; Remember this?

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

  ;; Yikes!




















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





















  ;; Transducers to the rescue!



















  (list-transactions (account-id "Sam"))



















  ;; Great, now how much money do I have?

















  ;; A little refactoring first...

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

















  (defn list-transactions [account-number]
    (transduce (transaction-lister account-number) conj @ledger))


















  (defn get-balance
    ([account-number]
     (transduce (comp (transaction-lister account-number)
                      (map :amount))
                +
                @ledger)))


















  (get-balance (account-id "Sam"))


















  ;; And how much did I have last Tuesday?

















  (Instant/parse "2020-11-24T15:00:00.0Z")


















  (.compareTo (Instant/parse "2020-11-24T15:00:00.0Z")
              (Instant/now))


















  (.compareTo (Instant/now)
              (Instant/parse "2020-11-24T15:00:00.0Z"))




















  (let [now (Instant/now)]
    (.compareTo now now))

















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


















  (execute-credit-transfer (account-id "Ashley")
                           (account-id "Sam")
                           42)


















  (get-balance (account-id "Sam"))


















  (get-balance (account-id "Sam")
               "2020-12-02T00:00:00Z")



















  ;; What if we have millions of transactions?


















  (def balances (atom {}))


















  (defn update-balances []
    (->> (keys @accounts)
         (map (fn [account-number]
                [account-number (get-balance account-number)]))
         (into {})
         (reset! balances)))


















  @balances


















  (update-balances)


















  (defn get-cached-balance [account-number]
    (get @balances account-number))

















  (get-balance (account-id "Sam"))


















  (get-cached-balance (account-id "Sam"))

















  (execute-credit-transfer (account-id "Sam")
                           (account-id "Ashley")
                           5)



















  (get-balance (account-id "Sam"))

















  (get-cached-balance (account-id "Sam"))


















  ;; How can we keep the cache in sync?


















  (add-watch ledger :balance-updater
             (fn [_key _atom _old_state new-state]
               (let [{:keys [debit-account credit-account amount]} (first (rseq new-state))]
                 (swap! balances update debit-account (fnil - 0) amount)
                 (swap! balances update credit-account (fnil + 0) amount))))



















  (update-balances)



















  (get-balance (account-id "Sam"))


















  (get-cached-balance (account-id "Sam"))



















  (execute-credit-transfer (account-id "Sam")
                           (account-id "Ashley")
                           45)


















  (get-balance (account-id "Sam"))


















  (get-cached-balance (account-id "Sam"))













  )
