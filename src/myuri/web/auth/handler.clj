(ns myuri.web.auth.handler
  (:require [buddy.hashers :as hashers]
            [malli.core :as malli]
            [malli.error :as me]
            [myuri.model :as model]
            [myuri.web.auth.views :as av]
            [ring.util.response :as resp]
            [ring.util.response]
            [myuri.web.utils :refer [is-post?]]))


(defn check-user-password
  "docstring"
  [ds username password]
  (when-let [user (model/get-account ds username)]
    (when (hashers/verify password (:users/password_digest user))
      user)))


(defn login-handler
  "docstring"
  [{:keys [ds] :as req}]
  (if-not (is-post? req)
    (av/login-view req)
    (let [{:keys [username password]} (:params req)]
      (if-let [user (check-user-password ds username password)]
        (-> (resp/redirect "/")
            (assoc :session {:identity {:id       (:users/id user)
                                        :username (:users/username user)
                                        :email    (:users/email user)}}))
        (av/login-view req true)))))

(defn logout
  "docstring"
  [{session :session}]
  (-> (resp/redirect "/auth/login")                         ; Redirect to login
      (assoc :session (dissoc session :identity))))

(def User-Registration
  (malli/schema [:map
                 [:username [:fn {:error/message "username must consist of lowercase a-z, 0-9, '-', '_' and should be 3-20 characters long"}
                             (partial re-matches #"^[a-z0-9\-_]{3,20}$")]]
                 [:email [:fn {:error/message "please provide a valid email address"}
                          (partial re-matches #"^.+@.{2,}\..{2,}$")]]
                 [:password [:string {:min 10}]]]))


(defn register-handler
  "docstring"
  [{:keys [ds] :as req}]

  (if-not (is-post? req)
    (av/register-view req)
    (let [params (-> req :params)
          user (-> params
                   (select-keys [:username :email :password]))]
      (if-let [errors (-> User-Registration
                          (malli/explain user)
                          (me/humanize))]
        (av/register-view (assoc req :validation/errors errors))
        (do
          (model/create-user ds nil user)
          (resp/redirect "/auth/login"))))))

(defn unauthorized-handler
  "docstring"
  [req _]
  (resp/redirect (str "/auth/login")))