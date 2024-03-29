(ns myuri.web.bookmarks.views
  (:require [myuri.web.utils :as u]
            [myuri.web.views :as l]
            [ring.util.anti-forgery :refer [anti-forgery-field]])
  (:import (java.text SimpleDateFormat)))


(defn new-bookmark-view
  "docstring"
  [req collections]
  (let [{:keys [su st p]} (-> req :params)
        frame (if p l/site l/layout)]

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
               [:input.button.is-link {:type "submit" :value "create" :autofocus true}]]]]])))

(defn edit-bookmark-view
  "docstring"
  [req bm]
  (l/layout req
            [:div.container {:style "margin-top: 20px;"}
             [:h3.is-size-3 "Edit Bookmark"]
             [:div {:style "padding: 10px"}
              [:form {:action (str "/bookmarks/" (:bookmarks/id bm) "/edit") :method "post"}
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
  (or (not-empty (:title bm))
      (:url bm)))

(defn bookmarks-table
  "docstring"
  [req bookmarks]
  (for [bm bookmarks]
    (let [title (title-or-url bm)
          url (:url bm)]
      [:div.bm-item
       [:a {:href url :target "_blank" :title url}
        [:div #_[:img.site-icon {:src (str (u/domain-from-url url true) "/favicon.ico")}] title]
        [:div {:style "margin: -4px 0 2px 0; font-size: 12px; color: #889"} (u/domain-from-url url) " " #_[:span {:style "color: red; background-color: #ffcccc; padding: 2px;"} "404"]]]
       [:div.bm-footer
        [:span {:class "date"} (format-date (:created_at bm))]
        " — "
        [:a {:href (format "/bookmarks/%s/edit" (-> bm :id str)) :class "edit-bm"} "edit"]
        " | "
        [:a {:href (format "/bookmarks/%s" (-> bm :id str)) :hx-target "closest div.bm-item" :hx-swap "delete" :hx-delete (format "/bookmarks/%s" (-> bm :id str))} "delete"]]])))


(defn index-view
  "docstring"
  [req {:keys [bookmarks collections]}]
  (l/layout req
            [:div.container {:style "margin-top: 30px;"}
             #_[:div.select
                [:select {:name "collection_selector"}
                 (for [c collections]
                   [:option {:value (:id c)} (:name c)])]]
             (bookmarks-table req bookmarks)]))
