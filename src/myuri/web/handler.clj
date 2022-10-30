(ns myuri.web.handler
  (:require [bidi.ring :refer [make-handler]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.response :as res]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response wrap-json-params]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [buddy.auth.backends :as backends]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [cheshire.core :as json]
            [myuri.db :as db]
            [myuri.model :as m]
            [myuri.web.auth.handler :as ah]
            [myuri.web.views :as v]
            [myuri.web.utils :refer [user-id is-post?]]
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

(defn create-bookmark
  "docstring"
  [ds user-id bm]
  (db/store! ds {:site_url   (:url bm)
                 :site_title (:title bm)
                 :user_id    user-id}))

(defn new-bookmark-handler
  "docstring"
  [{:keys [ds] :as req}]
  (if (is-post? req)
    (let [{:keys [su st p]} (:params req)
          user-id (user-id req)]
      (create-bookmark ds user-id {:url   su
                                   :title st})
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
                                             :bookmarks/site_url   su})
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

(defn token-settings-handler
  "docstring"
  [req]
  (v/settings-layout
    req
    [:h3.is-size-3 "Tokens"] [:button.button.is-primary "Create Token"]
    [:table.table.is-fullwidth.is-hoverable
     [:thead
      [:tr
       [:th "Name"]
       [:th "Id"]
       [:th "Token"]
       [:th "Valid until"]
       [:th ""]]]
     [:tbody
      [:tr
       [:td "iOS App"]
       [:td "98e9a"]
       [:td "gKWUrdDNNNrdy3psURELxSdb2NprCtIUxd97e5sC"]
       [:td ""]
       [:td [:a {:href "#"} "delete"]]]]]))


;; Routes and Middlewares -----------------------------------------------------
(def web-routes
  ["/" {""                 index-handler
        "new"              new-bookmark-handler
        ["bookmarks/" :id] {:delete {"" delete-bookmark-handler}
                            "/edit" edit-bookmark-handler}
        "backup"           {""    backup-endpoint
                            :post {"/export" export-handler}}
        "auth/"            {"login"    ah/login-handler
                            :post      {"logout" ah/logout}
                            "register" ah/register-handler}
        "settings"         {""        (fn [req] (res/redirect "/settings/tokens"))
                            "/tokens" token-settings-handler}
        "api/bookmarks"    {"" (fn [req]
                                 (res/response
                                   {:response "ok"}))}
        true               not-found-handler}])

(defn wrap-system
  "Injects System components into the request map"
  [handler opts]
  (fn [req]
    (-> req
        (assoc :ds (:ds opts))
        (handler))))


(def cookie-backend (backends/session {:unauthorized-handler unauthorized-handler}))
(defn token-backend
  [ds]
  (backends/token {:authfn ah/token-auth}))

(def authz-rules [{:pattern #"^/auth/.*" :handler (constantly true)} ; Let everyone use the auth endpoints
                  {:pattern #"^/.*" :handler authenticated?}])

(defn wrap-auth
  "docstring"
  [handler rules & backends]
  (-> handler
      (wrap-access-rules {:rules rules})
      ((fn [h] (apply wrap-authentication h backends)))
      (wrap-authorization cookie-backend)))

(defn wrap-site-defaults
  "docstring"
  [handler opts]
  (let [cookie-stor (cookie-store {:key (:cookie-secret opts)})
        defaults (-> site-defaults
                     (assoc-in [:session :store] cookie-stor)
                     (assoc-in [:session :cookie-attrs :same-site] :lax)
                     (assoc-in [:security :anti-forgery] false))]
    (wrap-defaults handler defaults)))

(defn new-handler
  [opts]
  (-> (make-handler web-routes)
      (wrap-auth authz-rules cookie-backend (token-backend (:ds opts)))
      (wrap-system opts)
      (wrap-site-defaults opts)
      #_(wrap-cors :access-control-allow-origin #".*"
                   :access-control-allow-methods [:get :put :post :delete])
      (wrap-json-params {:keywords? true :bigdecimals? true})
      (wrap-json-response)
      (wrap-reload)))