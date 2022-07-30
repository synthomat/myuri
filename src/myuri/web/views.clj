(ns myuri.web.views
  (:require [hiccup.page :as hp]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.util.response :as resp])
  (:import (java.text SimpleDateFormat)))

(defn app-address
  "docstring"
  [req]
  (str (-> req :scheme name) "://" (:server-name req) ":" (:server-port req)))

(defn bookmarklet-address
  "docstring"
  [app-address]
  (str "javascript:window.open('" app-address "/new?p=1&su='+encodeURIComponent(document.location.href)+'&st='+document.title, '', 'width=500,height=250')"))


(defn site
  "Base page skeleton"
  [req & children]
  (-> (hp/html5
        [:head
         (hp/include-js "/js/app.js")
         (hp/include-css "https://unpkg.com/turretcss/dist/turretcss.min.css")
         (hp/include-css "/css/app.css")
         [:script (str "const csrfToken = '" (:anti-forgery-token req) "';")]]
        [:body
         children])
      (resp/response)
      (resp/content-type "text/html")))

(defn navigation
  "Navigation component"
  [req]
  [:nav.nav-inline
   [:ul
    [:li [:a {:href "/"} "Home"]]
    [:li [:a {:href "/new"} "New"]]
    [:li [:a {:href "/backup"} "Backup"]]
    [:li " – Bookmarklet: ["
     [:a {:href (bookmarklet-address (app-address req))} "Save"]
     "]"]]])

(defn header
  "docstring"
  [req]
  [:div.page-header
   [:h1.logo "myuri" [:span {:style "color: red"} "*"]]
   (navigation req)])

(defn layout
  "Layout with Header (Navigation), etc..."
  [req & children]

  (site req
        (header req)

        [:div.container
         children]))

(defn new-bookmark-view
  "docstring"
  [req]
  (let [{:keys [su st p]} (-> req :params)
        frame (if p site layout)]

    (frame req
           [:form {:action "/new" :method "post"}
            (anti-forgery-field)
            [:input {:type "hidden" :name "p" :value p}]
            [:p "URL:" [:br]
             [:input {:type "text" :name "su" :value su :required true :minlength 12 :size 50}]]
            [:p "Title:" [:br]
             [:input {:type "text" :name "st" :value st :size 50}]]
            [:p [:input {:type "submit" :value "Create"}]]])))


(defn format-date
  "docstring"
  ([date] (format-date date "yyyy-MM-dd"))
  ([date format] (.format (SimpleDateFormat. format) date)))

(defn bookmarks-table
  "docstring"
  [req bookmarks]
  (for [bm bookmarks]
    [:div.bm-item
     [:a {:href (:bookmarks/site_url bm) :target "_blank" :title (:bookmarks/site_url bm)} (:bookmarks/site_title bm)]
     [:div.footer
      [:span {:class "date"} (format-date (:bookmarks/created_at bm))]
      " — "
      ;[:a {:href (format "/bookmarks/%d/edit" (:bookmarks/id bm)) :class "edit-bm" :data-bm-id (:bookmarks/id bm)} "EDIT"]
      ; " | "
      [:a {:href (format "/bookmarks/%d" (:bookmarks/id bm)) :class "delete-bm" :data-bm-id (:bookmarks/id bm)} "DEL"]]]))


(defn index-view
  "docstring"
  [req bookmarks]
  (layout req
          [:h1 "Bookmarks"]
          (bookmarks-table req bookmarks)))

(defn backup-view
  [req]
  (layout req
          [:div

           [:div
            [:h2 "Backup"]
            [:form {:action "/backup/export" :method "post"}
             (anti-forgery-field)
             [:input {:type "submit" :value "Download Export"}]]]

           #_[:div {:style "margin-top: 50px"}
              [:h2 "Restore"]
              [:form {:action "/backup/import" :method "post"}
               (anti-forgery-field)
               [:input {:type "file" :name "data"}]
               [:input {:type "submit" :value "Import"}]]]]))