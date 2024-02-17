(ns myuri.web.bookmarks.handler
  (:require [myuri.db :as db]
            [myuri.model :as m]
            [myuri.web.bookmarks.views :as v]
            [myuri.web.utils :refer [is-post? user-id]]
            [myuri.web.views :as l]
            [clj-http.client :as http]
            [ring.util.response :as resp]))

;; Handlers -------------------------------------------------------------------


(defn model->bm
  "docstring"
  [m]
  {:id         (:bookmarks/id m)
   :title      (or (not-empty (:bookmarks/site_title m))
                   (:bookmarks/site_url m))
   :url        (:bookmarks/site_url m)
   :created_at (:bookmarks/created_at m)})

(defn model->col
  "docstring"
  [m]
  {:id         (:collections/id m)
   :name       (:collections/name m)
   :created_at (:collections/created_at m)})

(defn bm->model
  "docstring"
  [bm]
  {:site_url   (:url bm)
   :site_title (:title bm)})

(defn json-resp
  "docstring"
  [res]
  (-> (resp/response res)
      (resp/content-type "application/json")))

(defn api-index-handler
  [{:keys [ds] :as req}]
  (let [query (-> req :params :q)
        bookmarks (db/bookmarks ds (user-id req) {:q query})]
    (-> (json-resp (map model->bm bookmarks))
        (resp/header "X-Total-Count" (count bookmarks)))))


(defn api-bookmark-handler
  [{:keys [bookmark]}]
  (json-resp (-> bookmark
                 model->bm)))

(defn api-update-bookmark-handler
  "docstring"
  [{:keys [ds params bookmark]}]
  (let [bm-id (:id bookmark)
        data (select-keys params [:title :url])
        updated-data (-> bookmark
                         model->bm
                         (merge data))]
    (m/update-bookmark ds bm-id (bm->model updated-data))
    (json-resp (-> updated-data))))


(defn api-create-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]

  (let [bm (select-keys params [:title :url])
        mapped {:site_title (:title bm)
                :site_url   (:url bm)
                :user_id (user-id req)}]
    (if-let [res (db/store! ds mapped)]
      (json-resp (model->bm res)))))

(defn index-handler
  [req]
  (let [bookmarks (-> (api-index-handler req) :body)]
    (v/index-view req {:bookmarks   bookmarks})))


(defn create-bookmark
  "docstring"
  [ds user-id bm]
  (db/store! ds {:site_url   (:url bm)
                 :site_title (:title bm)
                 :user_id    user-id}))

(defn new-bookmark-handler
  "docstring"
  [{:keys [ds] :as req}]
  (if-not (is-post? req)
    (v/new-bookmark-view req)
    (let [{:keys [su st p] :as bm} (:params req)
          user-id (user-id req)]
      (when (create-bookmark ds user-id {:url   su
                                       :title st})
        (future (println "Getting meta" bm)))
      (if (= p "1")
        (l/site req
                [:h2 "You may close this popup now"]
                [:script "window.onload = window.close"])
        (resp/redirect "/")))))

(defn delete-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]
  (let [bookmark-id (-> params :id parse-uuid)]
    (if (db/delete! ds (user-id req) bookmark-id)
      (resp/status 200)
      (-> (resp/response "Something bad happened")
          (resp/status 500)))))

(defn edit-bookmark-handler
  "docstring"
  [{:keys [ds params] :as req}]
  (let [bookmark-id (-> params :id parse-uuid)
        user-id (user-id req)]
    (if-let [bm (m/bookmark-by-id ds user-id bookmark-id)]
      (if-not (is-post? req)
        (v/edit-bookmark-view req bm)
        (let [{:keys [su st]} (:params req)]
          (m/update-bookmark ds bookmark-id {:bookmarks/site_title st
                                             :bookmarks/site_url   su})
          (resp/redirect "/")))

      (-> (l/layout req "Bookmark not found")
          (resp/status 404)))))