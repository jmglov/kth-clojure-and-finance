(ns kth-clj-finance.accounts
  (:import (java.time Instant)))

(defn new-account-number []
  (format "%07d" (rand-int 9999999)))

(defn create-account [account-holder]
  {:id (new-account-number)
   :account-holder account-holder
   :date-opened (str (Instant/now))})
