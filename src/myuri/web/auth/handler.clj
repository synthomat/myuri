(ns myuri.web.auth.handler
  (:require [buddy.hashers :as hashers]
            [malli.core :as malli]
            [malli.error :as me]
            [myuri.db :as db]
            [myuri.model :as model]
            [myuri.web.utils :refer [is-post?]]
            [ring.util.codec :refer [url-decode url-encode]]
            [ring.util.response :as resp]
            [selmer.parser :refer [render-file]]))


(defn check-user-password
  "docstring"
  [ds username password]
  (when-let [user (model/get-account ds username)]
    (when (hashers/check password (get user :users/password_digest))
      (dissoc user :users/password_digest))))

(defn tpl-resp
  "docstring"
  [template data]
  {:selmer {:template template
            :data     data}})


(defn make-identity
  "docstring"
  [user]
  {:id       (:users/id user)
   :username (:users/username user)
   :email    (:users/email user)})

(defn login-handler-post
  "docstring"
  [{:keys                                  [ds] :as req
    {{:keys [username password to]} :form} :parameters}]
  (if-let [user (check-user-password ds username password)]
    (let [next (url-decode (or (not-empty to) "/"))
          identity (make-identity user)]
      (-> (resp/redirect next)
          (assoc :session {:identity identity})))
    (tpl-resp "auth/login.html" {:req      req
                                 :username username
                                 :password password
                                 :error    true})))

(defn login-handler-get
  "docstring"
  [{:keys                 [ds] :as req
    {{:keys [to]} :query} :parameters}]
  (tpl-resp "auth/login.html" {:req req}))


(defn logout-handler
  "docstring"
  [{session :session}]
  (-> (resp/redirect "/auth/login")                         ; Redirect to login
      (assoc :session (dissoc session :identity))))

(def User-Registration
  (malli/schema
    [:map
     [:username [:fn {:error/message "Username must consist of lowercase a-z, 0-9, '-', '_' and should be 3-20 characters long"}
                 (partial re-matches #"^[a-z0-9\-_]{3,20}$")]]
     [:email [:fn {:error/message "Please provide a valid email address"}
              (partial re-matches #"^.+@.{2,}\..{2,}$")]]
     [:password [:string {:min 10}]]]))

(defn validate-model
  "docstring"
  [model data]
  (-> model
      (malli/explain data)
      (me/humanize)))

(defn register-handler
  "docstring"
  [{:keys [ds params] :as req}]

  (if-not (is-post? req)
    (tpl-resp "auth/register.html" {:req req})

    (let [user (select-keys params [:username :email :password])]
      (if-let [errors (validate-model User-Registration user)]
        (tpl-resp "auth/register.html" {:req (assoc req :validation/errors errors)})

        (if (model/user-exists? ds user)
          (tpl-resp "auth/register.html" {:req   req
                                          :error "The provided username or email address already exist."})
          (do
            (model/create-user! ds nil user)
            (resp/redirect "/auth/login")))))))



(defn unauthorized-handler
  "Default action on unauthorized event -> redirect to login-page"
  [req _]
  (let [redirect (url-encode (str (:uri req) "?" (:query-string req)))]
    (resp/redirect (str "/auth/login?to=" redirect))))