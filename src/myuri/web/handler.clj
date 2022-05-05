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



(def routes
  ["/" {""                          index-handler
        "new"                       new-bookmark-handler
        ["bookmarks/" [#"\d+" :id]] {:delete {"" delete-bookmark-handler}}
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
