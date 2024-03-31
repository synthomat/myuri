(ns myuri.web.handler
  (:require [cheshire.core :as json]
            [myuri.db :as db]
            [myuri.model :as m]
            [myuri.web.utils :refer [is-post? user-id]]

            [selmer.parser :refer [render-file]]
            [ring.util.response :as resp])
  (:import (java.text SimpleDateFormat)
           (java.time.format DateTimeFormatter)))

(selmer.parser/set-resource-path! (clojure.java.io/resource "templates"))

(selmer.parser/cache-off!)

(defn format-date
  "docstring"
  [format date]
  (-> format
      DateTimeFormatter/ofPattern
      (.format date)))

(defn format-date
  "docstring"
  ([date] (format-date date "yyyy-MM-dd"))
  ([date format] (.format (SimpleDateFormat. format) date)))

(defn tpl-resp
  "docstring"
  [template data]
  {:selmer {:template template
            :data     data}})

;; Handlers -------------------------------------------------------------------
(defn model->bm
  "docstring"
  [m]
  {:id         (:bookmarks/id m)
   :title      (or (not-empty (:bookmarks/site_title m))
                   (:bookmarks/site_url m))
   :url        (:bookmarks/site_url m)
   :created_at (:bookmarks/created_at m)})

(defn bm->model
  "docstring"
  [bm]
  {:site_url   (:url bm)
   :site_title (:title bm)})

(defn html-response
  "docstring"
  [template data]
  (-> (render-file template data)
      resp/response
      (resp/content-type "text/html")))


(defn index-handler
  [{:keys [ds] :as req}]
  (let [query (-> req :params :q)
        bookmarks (db/bookmarks ds (user-id req) {:q query})
        bms (map model->bm bookmarks)]
    (tpl-resp "index.html" {:bookmarks bms
                            :req       (assoc req :authenticated true)})))

(defn create-bookmark
  "docstring"
  [ds user-id bm]
  (db/store! ds {:site_url   (:url bm)
                 :site_title (:title bm)
                 :user_id    user-id}))

(defn new-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]
  (let [p (:p params)]

    (if-not (is-post? req)
      (html-response (if p
                       "new-bookmark.html"
                       "new-bookmark.html")
                     {:req (assoc req :authenticated true)})
      (let [{:keys [su st p] :as bm} (:params req)
            user-id (user-id req)]
        (when (create-bookmark ds user-id {:url   su
                                           :title st})
          (future (println "Getting meta" bm)))
        (if (= p "1")
          (html-response "index.html" nil)
          #_(l/site req
                    [:h2 "You may close this popup now"]
                    [:script "window.onload = window.close"])
          (resp/redirect "/"))))))

(defn edit-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]
  (let [bookmark-id (-> params :id parse-uuid)
        user-id (user-id req)]
    (if-let [bm (m/bookmark-by-id ds user-id bookmark-id)]
      (if-not (is-post? req)
        (html-response "edit-bookmark.html" {:req (assoc req :authenticated true)
                                             :bm  bm})
        (let [{:keys [su st]} (:params req)]
          (m/update-bookmark ds bookmark-id {:bookmarks/site_title st
                                             :bookmarks/site_url   su})
          (resp/redirect "/")))


      (-> (html-response "error-404.html" nil)
          (resp/status 404)))))


(defn delete-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]
  (let [bookmark-id (-> params :id parse-uuid)]
    (if (db/delete! ds (user-id req) bookmark-id)
      (resp/status 200)
      (-> (resp/response "Something bad happened")
          (resp/status 500)))))


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
