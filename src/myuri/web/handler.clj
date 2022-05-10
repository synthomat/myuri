(ns myuri.web.handler
  (:require [bidi.ring :refer [make-handler]]
            [ring.util.response :as res]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.reload :refer [wrap-reload]]
            [myuri.web.views :as v]
            [myuri.db :as db]
            [cheshire.core :as json])
  (:import (java.time.format DateTimeFormatter)
           (java.time LocalDate LocalDateTime)))

(defn index-handler
  [{:keys [ds] :as req}]

  (let [bookmarks (db/bookmarks ds)]
    (-> (v/index-view req bookmarks))))

(defn is-post?
  "docstring"
  [req]
  (= (-> req :request-method) :post))

(defn new-bookmark-handler
  "docstring"
  [{:keys [ds] :as req}]
  (if (is-post? req)
    (let [{:keys [su st p]} (:params req)]
      (db/store! ds {:site_url   su
                     :site_title st})
      (if (= p "1")
        (v/site req
                [:h2 "You may close this popup now"]
                [:script "window.onload = window.close"])
        (res/redirect "/")))
    (v/new-bookmark-view req)))

(defn not-found-handler
  "docstring"
  [req]
  (-> (v/layout req [:h2 {:style "color: red"} "Page not found"])
      (res/status 404)))

(defn parse-int [s]
  (Integer. (re-find #"[0-9]*" s)))

(defn delete-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]
  (let [bookmark-id (-> params :id parse-int)]
    (if (db/delete! ds bookmark-id)
      (res/status 204)
      (-> (res/response "Something bad happened")
          (res/status 500)))))

(defn backup-endpoint
  "docstring"
  [req]
  (v/layout req
            (v/backup-view req)))

(defn bm->map
  "docstring"
  [db-bookmarks]
  (map (fn [bm]
         {:site_url (:bookmarks/site_url bm)
          :site_title (:bookmarks/site_title bm)
          :created_at (:bookmarks/created_at bm)})
       db-bookmarks))

(defn format-date
  "docstring"
  [format date]
  (-> format
      DateTimeFormatter/ofPattern
      (.format date)))

(defn export-endpoint
  "docstring"
  [{:keys [ds params] :as req}]
  (let [data (db/bookmarks ds)
        mapped-data (bm->map data)
        ts (LocalDateTime/now)
        json-data (json/encode {:time      (str ts)
                                :bookmarks mapped-data} {:pretty true})
        file-name (str "myuri-backup_" (format-date "yyyyMMddHHmmss" ts))]
    (-> (res/response json-data)
        (res/header "Content-Disposition" (str "attachment; filename=\"" file-name ".json\""))
        (res/content-type "application/json"))))


(def routes
  ["/" {""                          index-handler
        "new"                       new-bookmark-handler
        ["bookmarks/" [#"\d+" :id]] {:delete {"" delete-bookmark-handler}}
        "backup"                    backup-endpoint
        "backup/export"             {:post {"" export-endpoint}}
        true                        not-found-handler}])

(defn wrap-system
  "docstring"
  [handler opts]
  (fn [req]
    (handler (assoc req :ds (:ds opts)))))

(defn new-handler
  [opts]
  (-> (make-handler routes)
      (wrap-system opts)
      (wrap-defaults site-defaults)
      (wrap-reload)))
