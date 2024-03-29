(ns myuri.web.auth.views
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]
            [myuri.web.views :as v]))

(defn login-view
  "docstring"
  [req & [err]]
  (let [{:keys [username]} (-> req :params)]
    (v/layout req
              [:div.container
               [:h3.title.is-3 "Log in"]
               (when err
                 [:div.notification.is-danger.is-light
                  [:strong "⚠️ Login failed: "] "we couldn't find such a username/password combination"])
               [:form {:action "/auth/login" :method "post"}
                (anti-forgery-field)
                (when-let [to (-> req :params :to)]
                  [:input {:type "hidden" :name "to" :value to}])
                [:div.field
                 [:label.label "Username"]
                 [:div.control
                  [:input.input {:type "text" :name "username" :required true :value username}]]]

                [:div.field
                 [:label.label "Password"]
                 [:div.control
                  [:input.input {:type "password" :name "password" :required true}]]]

                [:div.field
                 [:div.control
                  [:input.button.is-link {:type "submit" :value "login"}]
                  " or " [:a {:href "/auth/register"} "register"]]]]])))

(defn register-view
  "docstring"
  [req]
  (let [{:keys [username email]} (-> req :params)]
    (let [errs (:validation/errors req)]
      (v/layout req
                [:div.container
                 [:h3.title.is-3 "Register"]
                 [:form {:action "/auth/register" :method "post"}
                  (anti-forgery-field)
                  [:div.field
                   [:label.label "Username"]
                   [:div.control
                    [:input.input {:type "text" :name "username" :required true :value username :minLength 3 :maxLength 20}]]
                   (when-let [err (:username errs)]
                     [:p.help.is-danger (clojure.string/join "; " err)])]
                  [:div.field
                   [:label.label "Email"]
                   [:div.control
                    [:input.input {:type "email" :name "email" :value email :required true}]]
                   (when-let [err (clojure.string/join "; " (:email errs))]
                     [:p.help.is-danger err])]

                  [:div.field
                   [:label.label "Password"]
                   [:div.control
                    [:input.input {:type "password" :name "password" :required true :minLength 10}]]
                   (when-let [err (:password errs)]
                     [:p.help.is-danger (clojure.string/join "; " err)])]

                  [:div.field
                   [:div.control
                    [:input.button.is-link {:type "submit" :value "register"}]
                    " or " [:a {:href "/auth/login"} "log in"]]]]]))))