(ns myuri.web.auth.handler
  (:require [myuri.web.views :as v]))

(defn login-handler
  "docstring"
  [req]
  (v/layout req
            [:h1 "Login"]))
