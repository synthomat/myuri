(ns myuri.web.views
  (:require
    [buddy.auth :refer [authenticated?]]
    [hiccup.page :as hp]
    [myuri.web.utils :as u]
    [ring.util.anti-forgery :refer [anti-forgery-field]]
    [ring.util.response :as resp]))


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
    [:a.navbar-item {:href "/" :style "font-size: 1.4em; font-weight: bold;"} "myuri" [:span {:style "color: red"} "*"]]
    [:a.navbar-burger {:role "button" :aria-label "menu" :aria-expanded "false" :data-target "navMenu"}
     (repeat 3 [:span {:aria-hidden "true"}])]]

   [:div#navMenu.navbar-menu
    (if (authenticated? req)
      (list
        [:div.navbar-start
         [:a.navbar-item {:href "/"} "Home"]
         [:div.buttons
          [:a.button.is-small.is-light.is-link {:href "/new"} "New"]]]

        [:div.navbar-end
         [:div.navbar-item
          [:div.buttons
           [:a.button.is-small {:href (bookmarklet-address (u/app-address req))} "Bookmarklet"]]]
         [:div.navbar-item.has-dropdown.is-hoverable
          [:a.navbar-link (-> req :identity :username)]
          [:div.navbar-dropdown.is-right
           #_[:a.navbar-item {:href "/settings"} "Settings"]
           #_[:hr.navbar-divider]
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



;; Settings Views -------------------------------------------------------------

(defn uri-prefix
  "docstring"
  [req sub]
  (clojure.string/starts-with? (:uri req) sub))

(defn active
  "docstring"
  [req prefix]
  (and (uri-prefix req prefix) "is-active"))

(defn settings-nav
  "docstring"
  [req]
  (let [is-active (partial active req)]
    [:aside.menu
     [:p.menu-label "General"]
     [:ul.menu-list
      [:li [:a {:href "/settings/ui" :class (is-active "/settings/ui")} "User Interface"]]
      [:li [:a {:href "/settings/backup" :class (is-active "/settings/backup")} "Backup"]]
      [:li [:a {:href "/settings/import" :class (is-active "/settings/import")} "Import"]]]

     #_#_[:p.menu-label "Integrations"]
             [:ul.menu-list
              [:li [:a {:href "/settings/tokens" :class (is-active "/settings/tokens")} "Tokens"]]]]))

(defn settings-layout
  "docstring"
  [req & children]
  (layout req
          [:div.container {:style "margin-top: 30px"}
           [:h3.is-size-3 {:style "margin-bottom: 20px"} "Account Settings"]
           [:div.columns
            [:div.column.is-2 (settings-nav req)]

            [:div.column children]]]))

(defn backup-view
  [req]
  (settings-layout req
                   [:div

                    [:div
                     [:h3.title.is-3 "Backup"]
                     [:form {:action "/backup/export" :method "post"}
                      (anti-forgery-field)
                      [:input {:type "submit" :value "Download Export"}]]]

                    #_[:div {:style "margin-top: 50px"}
                       [:h2 "Restore"]
                       [:form {:action "/backup/import" :method "post"}
                        (anti-forgery-field)
                        [:input {:type "file" :name "data"}]
                        [:input {:type "submit" :value "Import"}]]]]))


(defn token-view
  "docstring"
  [req]

  (settings-layout
    req
    [:h3.is-size-3 "Tokens"] [:button.button.is-primary "Create Token"]

    [:table.table.is-fullwidth.is-hoverable
     [:thead
      [:tr
       [:th "Name"]
       [:th "Id"]
       [:th "Token"]
       [:th "Valid until"]
       [:th ""]]]
     [:tbody
      [:tr
       [:td "iOS App"]
       [:td "98e9a"]
       [:td "gKWUrdDNNNrdy3psURELxSdb2NprCtIUxd97e5sC"]
       [:td ""]
       [:td [:a {:href "#"} "delete"]]]]]))

(defn ui-settings-view
  "docstring"
  [req smap]

  (settings-layout
    req
    [:h3.title.is-3 "UI Config"]

    [:form.config-toggles
     #_[:div.field
        [:div.control
         [:label.checkbox
          [:input {:type "checkbox" :name "detail_fetching" :checked (:detail_fetching smap)}] " Details fetching"]]
        [:p.help "Given only the site url, it will fetch site details in the background"]]

     [:div.field
      [:div.control
       [:label.checkbox
        [:input {:type "checkbox" :name "display_icons" :checked (:display_icons smap)}] " Display site icons"]]
      [:p.help "Will load site icons from original location"]]]))


