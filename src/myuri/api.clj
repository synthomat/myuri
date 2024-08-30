(ns myuri.api
  (:require [buddy.hashers :as hashers]
            [myuri.db :as db]
            [myuri.model :as m]))

;; Bookmarks -------------------------------------------------------------------
(defn list-bookmarks
  "docstring"
  [ds user-id rfilter]
  (db/bookmarks ds user-id rfilter))

(defn create-bookmark
  "docstring"
  [ds user-id bookmark]
  (when (m/create-bookmark! ds user-id bookmark)
    (future (println "Getting meta" bookmark))))

(defn get-bookmark
  "docstring"
  [ds user-id bookmark-id]
  (m/bookmark-by-id ds user-id bookmark-id))

(defn update-bookmark
  "docstring"
  [ds user-id bookmark-id updated-bookmark]
  (m/update-bookmark ds bookmark-id updated-bookmark))

(defn delete-bookmark [ds user-id bookmark-id]
  (db/delete! ds user-id bookmark-id))

;; Settings / Security --------------------------------------------------------
(defn change-user-password
  "docstring"
  [ds user-id old-password new-password]
  (let [user (db/user ds user-id)]
    (if (hashers/check old-password (:users/password_digest user))
      (let [password-hash (hashers/derive new-password)]
        (db/update-user! ds user-id {:password_digest password-hash}))
      {:error :wrong_password
       :message "old password doesn't match current user password don't match"})))