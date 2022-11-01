(ns myuri.web.middleware
  (:require [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [buddy.auth.backends :as backends]
            [buddy.auth :refer [authenticated?]]
            [myuri.web.auth.handler :refer [unauthorized-handler token-auth]]
            [myuri.web.auth.handler :as ah]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-params]]))

(def cookie-backend (backends/session {:unauthorized-handler unauthorized-handler}))

(defn token-backend
  [ds]
  (backends/token {:authfn token-auth}))

(def authz-rules [{:pattern #"^/auth/.*" :handler (constantly true)} ; Let everyone use the auth endpoints
                  {:pattern #"^/.*" :handler authenticated?}])


(defn wrap-system
  "Injects System components into the request map"
  [handler opts]
  (fn [req]
    (-> req
        (assoc :ds (:ds opts))
        (handler))))

(defn wrap-auth
  "docstring"
  [handler rules & backends]
  (-> handler
      (wrap-access-rules {:rules rules})
      ((fn [h] (apply wrap-authentication h backends)))
      (wrap-authorization cookie-backend)))

(defn wrap-site-defaults
  "docstring"
  [handler opts]
  (let [cookie-stor (cookie-store {:key (:cookie-secret opts)})
        defaults (-> site-defaults
                     (assoc-in [:session :store] cookie-stor)
                     (assoc-in [:session :cookie-attrs :same-site] :lax)
                     (assoc-in [:security :anti-forgery] false))]
    (wrap-defaults handler defaults)))

(defn wrap-middlewares
  "docstring"
  [handler opts]
  (-> handler
      (wrap-auth authz-rules cookie-backend (token-backend (:ds opts)))
      (wrap-system opts)
      (wrap-site-defaults opts)
      #_(wrap-cors :access-control-allow-origin #".*"
                   :access-control-allow-methods [:get :put :post :delete])
      (wrap-json-params {:keywords? true :bigdecimals? true})
      (wrap-json-response)
      (wrap-reload)))