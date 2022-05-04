(ns myuri.web.handler
  (:require [bidi.ring :refer [make-handler]]
            [ring.util.response :as res]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.reload :refer [wrap-reload]]
            [myuri.web.views :as v]
            [myuri.db :as db]))

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
    (let [{:keys [site_url site_title]} (:params req)]
      (db/store! ds {:site_url site_url
                     :site_title site_title})
      (res/redirect "/"))
    (v/new-bookmark-view req)))

(defn not-found-handler
  "docstring"
  [req]
  (-> (v/layout [:h2 {:style "color: red"} "Page not found"])
      (res/status 404)))

(def routes
  ["/" {""    index-handler
        "new" new-bookmark-handler
        true  not-found-handler}])

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
