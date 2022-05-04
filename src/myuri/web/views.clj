(ns myuri.web.views
  (:require [hiccup.page :refer [html5]]
            [ring.util.response :as resp]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn header
  "docstring"
  []
  [:div
   [:h1 "Bookmarks"]
   [:ul
    [:li [:a {:href "/"} "Home"]]
    [:li [:a {:href "/new"} "New"]]]])

(defn layout
  [children]
  (->
    (html5
      [:body
       (header)

       [:div
        children]])
    (resp/response)
    (resp/content-type "text/html")))

(defn new-bookmark-view
  "docstring"
  [req]
  (let [{:keys [site_url site_title]} (-> req :params)]
    (->
      (html5
        [:form {:action "/new" :method "post"}
         (anti-forgery-field)
         [:p
          "URL: "
          [:input {:type "text" :name "site_url" :value site_url}]]
         [:p
          "Title: "
          [:input {:type "text" :name "site_title" :value site_title}]]
         [:p
          [:input {:type "submit" :value "Create"}]]])
      (resp/response)
      (resp/content-type "text/html"))))

(defn bookmarks-table
  "docstring"
  [req bookmarks]
  (for [bm bookmarks]
    [:div
     [:a {:href (:bookmarks/site_url bm) :target "_blank"} (:bookmarks/site_url bm)]
     [:br]
     (:bookmarks/site_title bm)]))

(defn index-view
  "docstring"
  [req bookmarks]
  (layout
    (bookmarks-table req bookmarks)))