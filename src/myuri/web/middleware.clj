(ns myuri.web.middleware
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :as baa]
            [buddy.auth.backends :as backends]
            [buddy.auth.backends :as bab]
            [buddy.auth.middleware :as bam]
            [myuri.web.auth.handler :refer [token-auth unauthorized-handler]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.util.response :as resp]
            [selmer.parser :refer [render-file]]))

(def cookie-backend (bab/session {:unauthorized-handler unauthorized-handler}))

(defn token-backend
  [ds]
  (backends/token {:authfn (token-auth ds)}))

(def authz-rules [{:pattern #"^/auth/.*" :handler (constantly true)} ; Let everyone use the auth endpoints
                  {:pattern #"^/.*" :handler authenticated?}])

(defn wrap-authorization
  "docstring"
  [handler]
  (bam/wrap-authorization handler cookie-backend))

(defn wrap-authentication
  "docstring"
  [handler]
  (bam/wrap-authentication handler cookie-backend))

(defn wrap-access-rules
  "docstring"
  [handler]
  (baa/wrap-access-rules handler {:rules authz-rules}))

(defn wrap-system
  "Injects System components into the request map"
  [handler opts]
  (fn [req]
    (-> req
        (assoc :ds (:ds opts))
        (handler))))

(defn wrap-template-response
  "docstring"
  [handler]
  (fn [req]
    (let [res (handler req)]
      (if-let [{:keys [template data]} (:selmer res)]
        (-> (render-file template data)
            resp/response
            (resp/content-type "text/html")
            (merge res))
        res))))


(defn wrap-session
  "docstring"
  [handler key]
  (ring.middleware.session/wrap-session handler {:store (cookie-store {:key key})}))