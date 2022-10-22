(ns myuri.web.handler
  (:require [bidi.ring :refer [make-handler]]
            [cheshire.core :as json]
            [myuri.db :as db]
            [myuri.model :as m]
            [myuri.web.auth.handler :as ah]
            [myuri.web.views :as v]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.response :as res]
            [buddy.auth.backends :as backends]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [myuri.web.utils :refer [user-id is-post?]]
            [ring.middleware.cors :refer [wrap-cors]]
            [myuri.web.auth.handler :refer [unauthorized-handler]])
  (:import (java.time.format DateTimeFormatter)))

;; Utils ----------------------------------------------------------------------

(defn not-found-handler
  "docstring"
  [req]
  (-> (v/layout req
                [:div.container {:style "margin-top: 20px;"}
                 [:h3.is-size-3 {:style "color: red"} "Page not found"]])
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
      (res/status 200)
      (-> (res/response "Something bad happened")
          (res/status 500)))))

(defn edit-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]
  (let [bookmark-id (-> params :id parse-uuid)
        user-id (user-id req)]
    (if-let [bm (m/bookmark-by-id ds user-id bookmark-id)]
      (if (is-post? req)
        (let [{:keys [su st]} (:params req)]
          (m/update-bookmark ds bookmark-id {:bookmarks/site_title st
                                             :bookmarks/site_url su})
          (res/redirect "/"))
        (v/edit-bookmark-view req bm))

      (-> (v/layout req "Bookmark not found")
          (res/status 404)))))

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
  [{:keys [ds] :as req}]

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
        ["bookmarks/" :id] {:delete {"" delete-bookmark-handler}
                            "/edit" edit-bookmark-handler}
        "backup"           {""    backup-endpoint
                            :post {"/export" export-handler}}
        "auth/"            {"login"    ah/login-handler
                            :post      {"logout" ah/logout}
                            "register" ah/register-handler}
        true               not-found-handler}])

(defn wrap-system
  "Injects System components into the request map"
  [handler opts]
  (fn [req]
    (-> req
        (assoc :ds (:ds opts))
        (handler))))


(def authn-backend (backends/session {:unauthorized-handler unauthorized-handler}))

(def authz-rules [{:pattern #"^/auth/.*" :handler (constantly true)} ; Let everyone use the auth endpoints
                  {:pattern #"^/.*" :handler authenticated?}])

(defn wrap-auth
  "docstring"
  [handler backend rules]
  (-> handler
      (wrap-access-rules {:rules rules})
      (wrap-authentication backend)
      (wrap-authorization backend)))

(defn wrap-site-defaults
  "docstring"
  [handler opts]
  (let [cookie-stor (cookie-store {:key (:cookie-secret opts)})
        defaults (-> site-defaults
                     (assoc-in [:session :store] cookie-stor)
                     (assoc-in [:session :cookie-attrs :same-site] :lax))]
    (wrap-defaults handler defaults)))


(defn new-handler
  [opts]
  (-> (make-handler routes)
      (wrap-auth authn-backend authz-rules)
      (wrap-system opts)
      (wrap-site-defaults opts)
      #_(wrap-cors :access-control-allow-origin #".*"
                 :access-control-allow-methods [:get :put :post :delete])
      (wrap-reload)))