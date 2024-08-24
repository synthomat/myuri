(ns myuri.api
  (:require [myuri.db :as db]
            [myuri.model :as m]))

;; Bookmarks -------------------------------------------------------------------
(defn list-bookmarks
  "docstring"
  [ds user-id rfilter]
  (db/bookmarks ds user-id rfilter))

(defn create-bookmark
  "docstring"
  [ds user-id bookmark]
  (when (m/create-bookmark! ds user-id bookmark)
    (future (println "Getting meta" bookmark))))

(defn get-bookmark
  "docstring"
  [ds user-id bookmark-id]
  (m/bookmark-by-id ds user-id bookmark-id))

(defn update-bookmark
  "docstring"
  [ds user-id bookmark-id updated-bookmark]
  (m/update-bookmark ds bookmark-id updated-bookmark))

(defn delete-bookmark [ds user-id bookmark-id]
  (db/delete! ds user-id bookmark-id))