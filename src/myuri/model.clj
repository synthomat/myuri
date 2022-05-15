(ns myuri.model
  (:require
    [myuri.db :as db])
  (:import (java.time LocalDateTime)
           (java.time.format DateTimeFormatter)))


;; Manage Bookmarks -----------------------------------------------------------


;; Backups / Export / Import --------------------------------------------------
(defn bm->map
  "Maps database objects of bookmarks into map structure"
  [db-bookmarks]
  (-> (fn [bm]
        {:site_url   (:bookmarks/site_url bm)
         :site_title (:bookmarks/site_title bm)
         :created_at (:bookmarks/created_at bm)})
      (map db-bookmarks)))


(defn export-bookmarks
  "Creates a data dump map from all bookmarks"
  [ds]
  (let [all-bookmarks (db/bookmarks ds)
        format-version "1"
        mapped-data (bm->map all-bookmarks)]
    {:time           (LocalDateTime/now)
     :size           (count mapped-data)
     :format_version format-version
     :bookmarks      mapped-data}))

(defn import-bookmarks
  "docstring"
  [ds data]
  )