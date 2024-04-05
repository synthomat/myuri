(ns myuri.web.middleware
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :as baa]
            [buddy.auth.backends]
            [buddy.auth.backends :as bab]
            [buddy.auth.middleware :as bam]
            [myuri.web.auth.handler :refer [unauthorized-handler]]
            [ring.middleware.session :as rms]
            [ring.middleware.session.cookie :as cookie]
            [myuri.web.templating :as tmpl]))

(def cookie-backend (bab/session {:unauthorized-handler unauthorized-handler}))


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

(defn cookie-store
  [key]
  (let [byte-key (byte-array (map byte key))]
    (cookie/cookie-store {:key byte-key})))

(defn wrap-session
  "docstring"
  [handler key]
  (rms/wrap-session handler {:store (cookie-store key)}))

(defn wrap-templating
  "docstring"
  [handler]
  (tmpl/wrap-template-response handler))