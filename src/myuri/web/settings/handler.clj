(ns myuri.web.settings.handler
  (:require [myuri.web.settings.views :as sv]
            [cheshire.core :as json]
            [myuri.model :as m]
            [myuri.web.utils :refer [user-id]]
            [ring.util.response :as resp])
  (:import (java.time.format DateTimeFormatter)))


(defn format-date
  "docstring"
  [format date]
  (-> format
      DateTimeFormatter/ofPattern
      (.format date)))

(defn backup-endpoint
  "docstring"
  [req]
  (sv/backup-view req))

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
  (sv/token-view req))

(defn ui-settings-handler
  "docstring"
  [{:keys [ds] :as req}]
  (let [us (m/get-user-setting ds (user-id req) nil)
        setting-map (->> (map (fn [s] [(keyword (:user_settings/setting_name s)) (:user_settings/json_value s)]) us)
                         (into {}))]
    (sv/ui-settings-view req setting-map)))

(defn config-toggle-handler
  "docstring"
  [{:keys [ds] :as req}]

  (m/update-user-setting ds (user-id req) (-> req :params))
  (-> (resp/response {:value "ok"})))