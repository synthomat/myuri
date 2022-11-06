(ns myuri.web.handler
  (:require [bidi.ring :refer [make-handler]]
            [ring.util.response :as res]
            [cheshire.core :as json]
            [myuri.db :as db]
            [myuri.model :as m]
            [myuri.web.auth.handler :as ah]
            [myuri.web.views :as v]
            [myuri.web.utils :refer [user-id is-post?]]
            [myuri.web.middleware :as mw]

            [ring.util.response :as resp]
            [myuri.web.utils :as u])
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
  (v/token-view req))

(defn ui-settings-handler
  "docstring"
  [{:keys [ds] :as req}]
  (let [us (m/get-user-setting ds(u/user-id req) nil)
        setting-map (->> (map (fn [s] [(keyword (:user_settings/setting_name s)) (:user_settings/json_value s)]) us)
                        (into {}))]
    (v/ui-settings-view req setting-map)))

(defn config-toggle-handler
  "docstring"
  [{:keys [ds] :as req}]

  (println (m/update-user-setting ds (u/user-id req) (-> req :params)))
  (-> (resp/response {:value "ok"})))

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
        "settings"         {""        (fn [req] (res/redirect "/settings/ui"))
                            "/tokens" token-settings-handler
                            "/ui"     ui-settings-handler}
        "api/"             {"bookmarks"          {"" (fn [req]
                                                       (res/response
                                                         {:response "ok"}))}
                            ["user/settings"] {:put {"" config-toggle-handler}}}

        true               not-found-handler}])


(defn new-handler
  [opts]
  (-> (make-handler web-routes)
      (mw/wrap-middlewares opts)))