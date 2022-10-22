(ns myuri.web.views
  (:require [hiccup.page :as hp]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.util.response :as resp]
            [buddy.auth :refer [authenticated?]]
            [myuri.web.utils :as u])
  (:import (java.text SimpleDateFormat)))


(defn bookmarklet-address
  "docstring"
  [app-address]
  (str "javascript:window.open('" app-address "/new?p=1&su='+encodeURIComponent(document.location.href)+'&st='+document.title, '', 'width=500,height=250')"))


(defn site
  "Base page skeleton"
  [req & children]
  (-> (hp/html5

        [:head
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
         [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/bulma@0.9.4/css/bulma.min.css"}]
         (hp/include-css "/css/app.css")
         (hp/include-js "https://unpkg.com/htmx.org@1.8.2")
         (hp/include-js "/js/app.js")

         [:title "Myuri"]
         [:script (str "const csrfToken = '" (:anti-forgery-token req) "';")]]
        [:body
         children])
      (resp/response)
      (resp/content-type "text/html")))

(defn navigation
  "Navigation component"
  [req]
  [:nav.navbar {:role "navigation" :aria-label "main navigation" :style "border-bottom: #eaeaea 1px solid;"}
   [:div.navbar-brand
    [:a.navbar-item {:href "/" :style "font-size: 1.4em; font-weight: bold;"} "myuri" [:span {:style "color: red"} "*"]]]

   [:div.navbar-menu
    (if (authenticated? req)
      (list
        [:div.navbar-start
         [:a.navbar-item {:href "/"} "Home"]]

        [:div.navbar-end
         [:div.navbar-item
          [:div.buttons
           [:a.button.is-small {:href (bookmarklet-address (u/app-address req))} "Bookmarklet"]]]
         [:div.navbar-item.has-dropdown.is-hoverable
          [:a.navbar-link (-> req :identity :username)]
          [:div.navbar-dropdown.is-right
           [:a.navbar-item {:href "/" :hx-post "/auth/logout" :hx-target "body"} "Log out"]]]])

      [:div.navbar-end
       [:div.navbar-item
        [:div.buttons
         [:a.button.is-light {:href "/auth/login"} "Log in"]]]])]])

(defn header
  "docstring"
  [req]
  [:div

   (navigation req)])

(defn layout
  "Layout with Header (Navigation), etc..."
  [req & children]

  (site req
        (header req)
        [:div.uk-container.uk-container-expand
         children]))

(defn new-bookmark-view
  "docstring"
  [req]
  (let [{:keys [su st p]} (-> req :params)
        frame (if p site layout)]

    (frame req
           [:div {:style "padding: 10px"}
            [:form {:action "/new" :method "post"}
             (anti-forgery-field)
             [:input {:type "hidden" :name "p" :value p}]

             [:div.field
              [:label.label "URL"]
              [:div.control
               [:input.input {:type "text" :name "su" :value su :required true}]]]

             [:div.field
              [:label.label "Title"]
              [:div.control
               [:input.input {:type "text" :name "st" :value st}]]]
             [:div.field
              [:div.control
               [:input.button.is-link {:type "submit" :value "create"}]]]]])))

(defn edit-bookmark-view
  "docstring"
  [req bm]
  (layout req
          [:div.container {:style "margin-top: 20px;"}
           [:h3.is-size-3 "Edit Bookmark"]
           [:div {:style "padding: 10px"}
            [:form {:action (str "/bookmarks/" (:bookmarks/id bm) "/edit")  :method "post"}
             (anti-forgery-field)

             [:div.field
              [:label.label "URL"]
              [:div.control
               [:input.input {:type "text" :name "su" :value (:bookmarks/site_url bm) :required true}]]]

             [:div.field
              [:label.label "Title"]
              [:div.control
               [:input.input {:type "text" :name "st" :value (:bookmarks/site_title bm)}]]]
             [:div.field
              [:div.control
               [:input.button.is-link {:type "submit" :value "update"}]]]]]]))

(defn format-date
  "docstring"
  ([date] (format-date date "yyyy-MM-dd"))
  ([date format] (.format (SimpleDateFormat. format) date)))


(defn title-or-url
  "Extracts the title from a bookmark record or falls back to url"
  [bm]
  (or (not-empty (:bookmarks/site_title bm))
      (:bookmarks/site_url bm)))

(defn bookmarks-table
  "docstring"
  [req bookmarks]
  (for [bm bookmarks]
    (let [title (title-or-url bm)
          url (:bookmarks/site_url bm)]
      [:div.bm-item
       [:a {:href url :target "_blank" :title url}
        [:div [:img.site-icon {:src (str (u/domain-from-url url true) "/favicon.ico")}] title]
        [:div {:style "margin: -4px 0 2px 0; font-size: 12px; color: #889"} (u/domain-from-url url)]]
       [:div.bm-footer
        [:span {:class "date"} (format-date (:bookmarks/created_at bm))]
        " — "
        [:a {:href (format "/bookmarks/%s/edit" (-> bm :bookmarks/id str)) :class "edit-bm"} "edit"]
        " | "
        [:a {:href (format "/bookmarks/%s" (-> bm :bookmarks/id str)) :hx-target "closest div.bm-item" :hx-swap "delete" :hx-delete (format "/bookmarks/%s" (-> bm :bookmarks/id str))} "delete"]]])))


(defn pagination
  "docstring"
  [req]
  [:ul.uk-pagination.uk-flex-center {:uk-margin "true"}
   [:li [:a {:href "#"} [:span {:uk-pagination-previous "true"}]]]
   [:li [:a {:href "#"} "1"]]
   [:li.uk-disabled [:span "…"]]
   [:li [:a {:href "#"} "5"]]
   [:li [:a {:href "#"} "6"]]
   [:li.uk-active [:span "7"]]
   [:li [:a {:href "#"} "8"]]
   [:li [:a {:href "#"} [:span {:uk-pagination-next "true"}]]]])

(defn index-view
  "docstring"
  [req bookmarks]
  (layout req
          [:div.container {:style "margin-top: 20px;"}
           [:h3.is-size-3 "Bookmarks"]
           (bookmarks-table req bookmarks)]

          #_(pagination req)))

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