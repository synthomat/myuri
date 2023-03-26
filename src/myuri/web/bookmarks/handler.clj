(ns myuri.web.bookmarks.handler
  (:require [ring.util.response :as res]
            [myuri.db :as db]
            [myuri.model :as m]
            [myuri.web.bookmarks.views :as v]
            [myuri.web.views :as l]
            [myuri.web.utils :refer [user-id is-post?]]))

;; Handlers -------------------------------------------------------------------

(defn index-handler
  [{:keys [ds] :as req}]
  (let [query (-> req :params :q)
        bookmarks (db/bookmarks ds (user-id req) {:q query})
        collections (db/collections-by-user ds (str (user-id req)))]
    (-> (v/index-view req bookmarks collections))))

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
        (l/site req
                [:h2 "You may close this popup now"]
                [:script "window.onload = window.close"])
        (res/redirect "/")))
    (let [collections (db/collections-by-user ds (str (user-id req)))]
      (v/new-bookmark-view req collections))))

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

      (-> (l/layout req "Bookmark not found")
          (res/status 404)))))