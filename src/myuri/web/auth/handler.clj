(ns myuri.web.auth.handler
  (:require [myuri.web.views :as v]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))


(defn login-handler
  "docstring"
  [req]
  (v/layout req
            [:h1 "Login"]))

(defn register-handler
  "docstring"
  [req]
  (v/layout req
            [:div.container
             [:h3.title.is-3 "Register"]
             [:form {:action "/auth/register" :method "post"}
              (anti-forgery-field)
              [:div.field
               [:label.label "Username"]
               [:div.control
                [:input.input {:type "text" :name "username"}]]]
              [:div.field
               [:label.label "Email"]
               [:div.control
                [:input.input {:type "email" :name "email"}]]]

              [:div.field
               [:label.label "Password"]
               [:div.control
                [:input.input {:type "password" :name "password"}]]]

              [:div.field
               [:div.control
                [:input.button.is-link {:type "submit" :value "register"}]
                " or " [:a {:href "/auth/login"} "log in"]]]]]))
