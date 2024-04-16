(ns myuri.web.handler
  (:require [cheshire.core :as json]
            [myuri.db :as db]
            [myuri.model :as m]
            [myuri.web.templating :refer [tpl-resp]]

            [myuri.web.utils :refer [is-post? user-id]]
            [ring.util.response :as resp]
            [selmer.parser])
  (:import (java.text SimpleDateFormat)
           (java.time.format DateTimeFormatter)))


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

;; Handlers -------------------------------------------------------------------
(defn model->bm
  "docstring"
  [m]
  {:id         (:bookmarks/id m)
   :title      (or (not-empty (:bookmarks/site_title m))
                   (:bookmarks/site_url m))
   :url        (:bookmarks/site_url m)
   :created_at (:bookmarks/created_at m)})

(defn index-handler
  [{:keys [ds] :as req}]
  (let [query (-> req :parameters :query :q)
        bookmarks (db/bookmarks ds (user-id req) {:q query})
        bms (map model->bm bookmarks)]
    (tpl-resp "index.html" {:bookmarks bms
                            :query query})))



(defn new-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]
  (let [su (get params "su")
        st (get params "st")
        p (get params "p")]

    (if-not (is-post? req)
      (tpl-resp "new-bookmark.html"
                {:su su, :st st, :p p})
      (let [user-id (user-id req)]
        (when (m/create-bookmark! ds user-id {:url   su
                                              :title st})
          (future (println "Getting meta" su)))
        (if (= p "1")
          (tpl-resp "close-window.html")
          (resp/redirect "/"))))))

(defn edit-bookmark-handler
  "docstring"
  [{:keys [ds bookmark params] :as req}]
  (if-not (is-post? req)
    (tpl-resp "edit-bookmark.html" {:bm bookmark})
    (let [su (get params "su")
          st (get params "st")]
      (m/update-bookmark ds (:bookmarks/id bookmark) {:site_title st
                                                      :site_url   su})
      (resp/redirect "/"))))


(defn delete-bookmark-handler
  "docstring"
  [{:keys [ds bookmark] :as req}]
  (if (db/delete! ds (user-id req) (:bookmarks/id bookmark))
    (resp/status 200)
    (-> (resp/response "Something bad happened")
        (resp/status 500))))


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
