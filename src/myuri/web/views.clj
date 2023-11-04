(ns myuri.web.views
  (:require
    [buddy.auth :refer [authenticated?]]
    [hiccup.page :as hp]
    [myuri.web.utils :as u]
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
         ; (hp/include-js "https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js")
         ;[:script {:defer true :src "https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js"}]
         (hp/include-js "https://unpkg.com/htmx.org@1.8.2"
                        "/js/app.js")
         ;(hp/include-js "https://cdnjs.cloudflare.com/ajax/libs/crypto-js/4.1.1/crypto-js.min.js")
         ;(hp/include-js "/js/jsencrypt.min.js")


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
         [:div.navbar-item
          [:a.button.is-small.is-light.is-link {:href "/new"} "New"]]
         [:div.navbar-item.column.is-12
          [:form {:action "/" :method "get"}
           [:input.input.is-small {:name "q" :type "text" :placeholder "Search…" :value (-> req :params :q)}]]]]

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
        [:div.container.container-expand
         children]))