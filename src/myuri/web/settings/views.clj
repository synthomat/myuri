(ns myuri.web.settings.views
  (:require [myuri.web.views :as v]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

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
  (v/layout req
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


