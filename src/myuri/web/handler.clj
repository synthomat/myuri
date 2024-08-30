(ns myuri.web.handler
  (:require [cheshire.core :as json]
            [myuri.db :as db]
            [myuri.web.templating :refer [tpl-resp]]
            [myuri.web.utils :as u]
            [myuri.web.utils :refer [is-post? user-id]]
            [ring.util.response :as resp]
            [clj-http.util :as hu]
            [selmer.parser]
            [myuri.api :as api]
            [myuri.web.render :as render]
            [clojure.pprint :as pp]))


(defn- model->bm
  "docstring"
  [m]
  {:id          (:bookmarks/id m)
   :title       (or (not-empty (:bookmarks/site_title m))
                    (:bookmarks/site_url m))
   :url         (:bookmarks/site_url m)
   :description (:bookmarks/site_description m)
   :created_at  (:bookmarks/created_at m)
   :checks      (:bookmarks/checks m)})

(defn- method-not-allowed
  "docstring"
  []
  (-> (resp/response "Method not allowed")
      (resp/status 405)))

;; Handlers --------------------------------------------------------------------
(defn index-handler
  [{:keys                [ds] :as req
    {{search :q} :query} :parameters}]
  (let [user-id (user-id req)
        bookmarks-list (api/list-bookmarks ds user-id {:q search})
        bookmarks (map model->bm bookmarks-list)
        user-settings (db/user-settings ds user-id [:target_blank])]
    (render/index {:bookmarks bookmarks
                   :query     search
                   :us        user-settings})))

(defn new-bookmark-handler
  "docstring"
  [{:keys                            [ds request-method] :as req
    {{:keys [data p]}         :query
     {:keys [close] :as form} :form} :parameters}]
  (let [data (json/parse-string data true)]
    (case request-method
      :get (tpl-resp "bookmarks/new-bm.html" (merge data
                                                    {:p p}))
      :post (when (api/create-bookmark ds (user-id req) (select-keys form [:url :title :description]))
              (if (= close 1)
                (tpl-resp "close-window.html")
                (resp/redirect "/"))))))

(defn edit-bookmark-handler
  "docstring"
  [{:keys               [ds bookmark params request-method] :as req
    {bid :bookmarks/id} :bookmark}]
  (case request-method
    :get (tpl-resp "bookmarks/edit-bm.html" {:bm bookmark})
    :post (let [{:strs [url title description]} params]
            (api/update-bookmark ds (user-id req) bid {:site_title       title
                                                       :site_url         url
                                                       :site_description description})
            (resp/redirect "/"))
    (method-not-allowed)))

(defn delete-bookmark-handler
  "docstring"
  [{:keys               [ds] :as req
    {bid :bookmarks/id} :bookmark}]
  (if (api/delete-bookmark ds (user-id req) bid)
    (resp/status 200)
    (-> (resp/response "Something bad happened")
        (resp/status 500))))

(defn- send-json-file
  "Makes the browser download the provided file"
  [data file-name]
  (-> (resp/response data)
      (resp/header "Content-Disposition" (format "attachment; filename=\"%s\"" file-name))
      ;(resp/content-type "application/json")
      ))

#_(defn export-handler
    "docstring"
    [{:keys [ds] :as req}]

    (let [export-data (m/export-bookmarks ds (user-id req))
          ts (:time export-data)
          file-name (format "myuri-export_%s.json" (format-date "yyMMddHHmm" ts))
          json-data (-> export-data
                        (assoc :time (format-date "yyyy-MM-dd HH:mm:ss" ts))
                        (json/encode {:pretty true}))]
      (send-json-file json-data file-name)))


;;

(defn settings-index
  "docstring"
  [{:keys          [ds request-method] :as req
    {:keys [form]} :parameters}]

  (let [user-id (u/user-id req)
        allowed-settings {:target_blank false}
        settings (db/user-settings ds user-id (keys allowed-settings))]
    (if (not= request-method :post)
      (tpl-resp "settings/index.html" {:us settings})
      (let [safe-settings (reduce-kv (fn [m k v]
                                       (assoc m k (get form k v)))
                                     {}
                                     allowed-settings)]
        (db/set-user-settings! ds user-id safe-settings)
        (resp/redirect "/settings")))))

(defn security-handler
  "docstring"
  [{:keys [ds request-method] :as req}]
  (let [user-id (u/user-id req)]
    (if (not= request-method :post)
      (tpl-resp "settings/security.html")
      (case (-> req :params :action)
        "password_change" (if-let [resp (api/change-user-password ds user-id (-> req :params :current_password) (-> req :params :new_password))]
                            (tpl-resp "settings/security.html" {:message "Password changed successfully!"})
                            (tpl-resp "settings/security.html"))
        :default (tpl-resp "settings/security.html"))))
  )