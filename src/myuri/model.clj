(ns myuri.model
  (:require [buddy.hashers :as hashers]
            [honey.sql :as hsql]
            [myuri.db :as db]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql])
  (:import (java.time LocalDateTime)))

;; Users ----------------------------------------------------------------------

(defn insert-user
  "Creates new user record"
  [ds user]
  (let [user-record (-> user
                        (assoc :password_digest (hashers/derive (:password user)))
                        (dissoc :password))]
    (->> {:insert-into :users
          :values      [user-record]}
         (hsql/format)
         (jdbc/execute-one! ds))))

(defn get-account
  "Creates new user record"
  [ds username]
  (if (clojure.string/includes? username "@")
    (sql/get-by-id ds :users username :email nil)
    (sql/get-by-id ds :users username :username nil)))


(defn send-verification-mail
  "docstring"
  [mailer-fn user]
  (mailer-fn {:to   (:email user)
              :body (format "Please confirm your account registration http://localhost:3000/auth/confirm?code=%s" (:verification_code user))}))

(defn create-user
  "docstring"
  [ds mailer user]
  (let [code (random-uuid)
        user (assoc user :verification_code code)]
    (jdbc/with-transaction [tx ds]
                           (insert-user tx user)
                           (send-verification-mail (fn [data])
                                                   user))))

;; Manage Bookmarks -----------------------------------------------------------
(defn bookmark-by-id
  "docstring"
  [ds user-id bookmark-id]
  (-> (sql/find-by-keys ds :bookmarks {:user_id user-id
                                       :id      bookmark-id})
      first))

(defn update-bookmark
  "docstring"
  [ds bookmark-id data]
  (-> (sql/update! ds :bookmarks data {:id bookmark-id})))


;; Backups / Export / Import --------------------------------------------------
(defn bm->map
  "Maps database objects of bookmarks into map structure"
  [db-bookmarks]
  (-> (fn [bm]
        {:id         (:bookmarks/id bm)
         :site_url   (:bookmarks/site_url bm)
         :site_title (:bookmarks/site_title bm)
         :created_at (:bookmarks/created_at bm)})
      (map db-bookmarks)))


(defn export-bookmarks
  "Creates a data dump map from all bookmarks"
  [ds user-id]
  (let [all-bookmarks (db/bookmarks ds user-id)
        format-version "1.1"
        mapped-data (bm->map all-bookmarks)]
    {:time           (LocalDateTime/now)
     :size           (count mapped-data)
     :format_version format-version
     :bookmarks      mapped-data}))

(defn import-bookmarks
  "docstring"
  [ds data])

