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
            [ring.util.response])
  (:import (java.time.format DateTimeFormatter)))

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

(defn parse-int [s]
  (Integer. (re-find #"[0-9]*" s)))

;; Handlers -------------------------------------------------------------------

(defn index-handler
  [{:keys [ds] :as req}]

  (let [bookmarks (db/bookmarks ds)]
    (-> (v/index-view req bookmarks))))

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

(defn delete-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]
  (let [bookmark-id (-> params :id parse-int)]
    (if (db/delete! ds bookmark-id)
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

  (let [export-data (m/export-bookmarks ds)
        ts (:time export-data)
        file-name (format "myuri-export_%s.json" (format-date "yyMMddHHmm" ts))
        json-data (-> export-data
                      (assoc :time (format-date "yyyy-MM-dd HH:mm:ss" ts))
                      (json/encode {:pretty true}))]
    (send-json-file json-data file-name)))

;; Routes and Middlewares -----------------------------------------------------
(def routes
  ["/" {""                          index-handler
        "new"                       new-bookmark-handler
        ["bookmarks/" [#"\d+" :id]] {:delete {"" delete-bookmark-handler}}
        "backup"                    backup-endpoint
        "backup/export"             {:post {"" export-handler}}
        "auth/login"                ah/login-handler
        "auth/register"             ah/register-handler
        true                        not-found-handler}])

(defn wrap-system
  "Injects System components into the request map"
  [handler opts]
  (fn [req]
    (-> req
        (assoc :ds (:ds opts))
        (handler))))

(defn new-handler
  [opts]
  (-> (make-handler routes)
      (wrap-system opts)
      (wrap-defaults site-defaults)
      (wrap-reload)))