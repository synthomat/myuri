(ns myuri.model
  (:require [buddy.hashers :as hashers]
            [honey.sql :as hsql]
            [honey.sql.helpers :as hh]
            [myuri.db :as db]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql])
  (:import (java.time LocalDateTime)
           (org.postgresql.util PGobject)))


(def bookmark
  [:map
   [:url :string?]])

;; Users ----------------------------------------------------------------------

(defn insert-user!
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
  "Fetches a user account either by username or email"
  [ds username]
  (if (clojure.string/includes? username "@")
    (sql/get-by-id ds :users username :email nil)
    (sql/get-by-id ds :users username :username nil)))

(defn user-exists?
  "docstring"
  [ds user]
  (-> (sql/query ds ["select * from users where lower(username) = lower(?) or lower(email) = lower(?) limit 1"
                     (:username user) (:email user)])
      first
      some?))

(defn send-verification-mail
  "docstring"
  [mailer-fn user]
  (mailer-fn {:to   (:email user)
              :body (format "Please confirm your account registration http://localhost:3000/auth/confirm?code=%s" (:verification_code user))}))

(defn create-user!
  "docstring"
  [ds mailer user]
  (let [code (random-uuid)
        user (assoc user :verification_code code)]
    (jdbc/with-transaction [tx ds]
                           (insert-user! tx user)
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

;; User Settings --------------------------------------------------------------


(defn get-user-setting
  "docstring"
  ([ds user-id name]
   (if (nil? name)
     (sql/find-by-keys ds :user_settings {:user_id user-id})
     (-> (sql/find-by-keys ds :user_settings {:user_id      user-id
                                              :setting_name name})
         first))))

(defn update-user-setting
  "docstring"
  ([ds user-id key-values]
   (let [values (-> (map (fn [s] {:user_id      user-id
                                  :setting_name (name (get s 0))
                                  :json_value   (doto (PGobject.)
                                                  (.setType "json")
                                                  (.setValue (db/->json (get s 1))))})
                         key-values)
                    vec)
         stat (-> (hh/insert-into :user_settings)
                  (hh/values values)
                  (hh/on-conflict :user_id :setting_name)
                  (hh/do-update-set :json_value)
                  (hh/returning :*)
                  hsql/format)]
     (jdbc/execute! ds stat))))