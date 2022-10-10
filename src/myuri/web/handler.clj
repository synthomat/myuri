(ns myuri.web.handler
  (:require [bidi.ring :refer [make-handler]]
            [cheshire.core :as json]
            [myuri.db :as db]
            [myuri.model :as m]
            [myuri.web.auth.handler :as ah]
            [myuri.web.views :as v]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.response :as res]
            [ring.util.response]
            [buddy.auth.backends :as backends]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.util.response :as resp]
            [myuri.web.utils :refer [user-id]]
            [ring.middleware.cors :refer [wrap-cors]])
  (:import (java.time.format DateTimeFormatter)))

(use 'clojure.pprint)

;; Utils ----------------------------------------------------------------------
(defn is-post?
  "docstring"
  [req]
  (= (-> req :request-method) :post))

(defn not-found-handler
  "docstring"
  [req]
  (-> (v/layout req [:h2 {:style "color: red"} "Page not found"])
      (res/status 404)))


;; Handlers -------------------------------------------------------------------

(defn index-handler
  [{:keys [ds] :as req}]
  (let [bookmarks (db/bookmarks ds (user-id req))]
    (-> (v/index-view req bookmarks))))

(defn new-bookmark-handler
  "docstring"
  [{:keys [ds] :as req}]
  (if (is-post? req)
    (let [{:keys [su st p]} (:params req)
          user-id (user-id req)]
      (db/store! ds {:site_url   su
                     :site_title st
                     :user_id    user-id})
      (if (= p "1")
        (v/site req
                [:h2 "You may close this popup now"]
                [:script "window.onload = window.close"])
        (res/redirect "/")))
    (v/new-bookmark-view req)))

(defn delete-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]
  (let [bookmark-id (-> params :id parse-uuid)]
    (if (db/delete! ds (user-id req) bookmark-id)
      (res/status 204)
      (-> (res/response "Something bad happened")
          (res/status 500)))))

;; Backups Handlers -----------------------------------------------------------
(defn format-date
  "docstring"
  [format date]
  (-> format
      DateTimeFormatter/ofPattern
      (.format date)))

(defn backup-endpoint
  "docstring"
  [req]
  (v/backup-view req))

(defn send-json-file
  "Makes the browser download the provided file"
  [data file-name]
  (-> (res/response data)
      (res/header "Content-Disposition" (format "attachment; filename=\"%s\"" file-name))
      (res/content-type "application/json")))

(defn export-handler
  "docstring"
  [{:keys [ds params] :as req}]

  (let [export-data (m/export-bookmarks ds (user-id req))
        ts (:time export-data)
        file-name (format "myuri-export_%s.json" (format-date "yyMMddHHmm" ts))
        json-data (-> export-data
                      (assoc :time (format-date "yyyy-MM-dd HH:mm:ss" ts))
                      (json/encode {:pretty true}))]
    (send-json-file json-data file-name)))


;; Routes and Middlewares -----------------------------------------------------
(def routes
  ["/" {""                 index-handler
        "new"              new-bookmark-handler
        ["bookmarks/" :id] {:delete {"" delete-bookmark-handler}}
        "backup"           {""    backup-endpoint
                            :post {"/export" export-handler}}
        "auth/"            {"login"    ah/login-handler
                            "logout"   ah/logout
                            "register" ah/register-handler}
        true               not-found-handler}])

(defn wrap-system
  "Injects System components into the request map"
  [handler opts]
  (fn [req]
    (-> req
        (assoc :ds (:ds opts))
        (handler))))

(defn unauthorized-handler
  "docstring"
  [req metadata]
  (resp/redirect (str "/auth/login?next=" (:uri req))))

(def backend (backends/session {:unauthorized-handler unauthorized-handler}))

(def rules [{:pattern #"^/auth/.*"
             :handler (constantly true)}
            {:pattern #"^/.*"
             :handler authenticated?}])

(defn wrap-auth
  "docstring"
  [handler]
  (-> handler
      (wrap-access-rules {:rules rules})
      (wrap-authentication backend)
      (wrap-authorization backend)))

(defn new-handler
  [opts]
  (-> (make-handler routes)
      (wrap-auth)
      (wrap-system opts)
      (wrap-defaults (-> site-defaults
                         (assoc-in [:session :store] (cookie-store
                                                       {:key "agtjrfokft5rs95g"}))
                         (assoc-in [:session :cookie-attrs :same-site] :lax)))
      (wrap-cors :access-control-allow-origin #".*"
                 :access-control-allow-methods [:get :put :post :delete])
      (wrap-reload)))