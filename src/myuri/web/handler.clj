(ns myuri.web.handler
  (:require [bidi.ring :refer [make-handler]]
            [cheshire.core :as json]
            [myuri.model :as m]
            [myuri.web.auth.handler :as ah]
            [myuri.web.bookmarks.handler :as bh]
            [myuri.web.middleware :as mw]
            [myuri.web.utils :refer [user-id]]
            [myuri.web.views :as v]

            [ring.util.response :as resp])
  (:import (java.time.format DateTimeFormatter)))

;; Utils ----------------------------------------------------------------------

(defn not-found-handler
  "docstring"
  [req]
  (-> (v/layout req
                [:div.container {:style "margin-top: 20px;"}
                 [:h3.is-size-3 {:style "color: red"} "Page not found"]])
      (resp/status 404)))


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
  (-> (resp/response data)
      (resp/header "Content-Disposition" (format "attachment; filename=\"%s\"" file-name))
      (resp/content-type "application/json")))

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
  (let [us (m/get-user-setting ds (user-id req) nil)
        setting-map (->> (map (fn [s] [(keyword (:user_settings/setting_name s)) (:user_settings/json_value s)]) us)
                         (into {}))]
    (v/ui-settings-view req setting-map)))

(defn config-toggle-handler
  "docstring"
  [{:keys [ds] :as req}]

  (println (m/update-user-setting ds (user-id req) (-> req :params)))
  (-> (resp/response {:value "ok"})))

;; Routes and Middlewares -----------------------------------------------------
(def web-routes
  ["/" {""                 bh/index-handler
        "new"              bh/new-bookmark-handler
        ["bookmarks/" :id] {:delete {"" bh/delete-bookmark-handler}
                            "/edit" bh/edit-bookmark-handler}
        "backup"           {""    backup-endpoint
                            :post {"/export" export-handler}}
        "auth/"            {"login"    ah/login-handler
                            :post      {"logout" ah/logout}
                            "register" ah/register-handler}
        "settings"         {""        (fn [req] (resp/redirect "/settings/ui"))
                            "/tokens" token-settings-handler
                            "/ui"     ui-settings-handler
                            "/backup" backup-endpoint}
        ;; API ----------------------------------------------------------------
        "api/"             {"bookmarks"       {"" (fn [req]
                                                    (resp/response
                                                      {:response "ok"}))}
                            ["user/settings"] {:put {"" config-toggle-handler}}}

        true               not-found-handler}])


(defn new-handler
  [opts]
  (-> (make-handler web-routes)
      (mw/wrap-middlewares opts)))