(ns myuri.web.views
  (:require [hiccup.page :as hp]
            [ring.util.response :as resp]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn app-address
  "docstring"
  [req]
  (str (-> req :scheme name) "://" (:server-name req) ":" (:server-port req)))

(defn bookmarklet-address
  "docstring"
  [app-address]
  (str "javascript:window.open('" app-address "/new?site_url='+encodeURIComponent(document.location.href)+'&site_title='+document.title, '', 'width=500,height=200')"))


(defn header
  "docstring"
  [req]
  [:div
   [:h1 "Bookmarks"]
   [:ul
    [:li [:a {:href "/"} "Home"]]
    [:li [:a {:href "/new"} "New"]]
    [:li [:a {:href (bookmarklet-address (app-address req))} "Save"]]]])

(defn site
  "docstring"
  [req & children]
  (-> (hp/html5
        [:head
         (hp/include-js "/js/app.js")
         (hp/include-css "/css/app.css")
         [:script (str "const csrfToken = '" (:anti-forgery-token req) "';")]]
        [:body
         children])
      (resp/response)
      (resp/content-type "text/html")))

(defn layout
  [req & children]

  (site req
    (header req)

    [:div
     children]))

(defn new-bookmark-view
  "docstring"
  [req]
  (let [{:keys [site_url site_title]} (-> req :params)]
    (site req
      [:form {:action "/new" :method "post"}
       (anti-forgery-field)
       [:p "URL:" [:br]
        [:input {:type "text" :name "site_url" :value site_url :required "" :minlength 12 :size 50}]]
       [:p "Title:" [:br]
        [:input {:type "text" :name "site_title" :value site_title :size 50}]]
       [:p [:input {:type "submit" :value "Create"}]]])))

(defn bookmarks-table
  "docstring"
  [req bookmarks]
  (for [bm bookmarks]
    [:div
     [:p
      [:a {:href (:bookmarks/site_url bm) :target "_blank"} (:bookmarks/site_url bm)]
      [:br]
      (:bookmarks/site_title bm)
      [:br]
      [:a {:href "#" :class "delete-bm" :data-bm-id (:bookmarks/id bm)} "delete"]]]))


(defn index-view
  "docstring"
  [req bookmarks]
  (let [bookmarklet-addr (bookmarklet-address (app-address req))]
    (layout req
            (bookmarks-table req bookmarks))))