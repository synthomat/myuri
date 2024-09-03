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

(defn any-role?
  "docstring"
  [req]
  (let [path-roles (-> req :reitit.core/match :data :roles)
        user-roles (-> req :identity :roles)]
    (prn path-roles)
    (some? (not-empty (clojure.set/intersection path-roles user-roles)))))

(defn is-admin?
  "docstring"
  [req]
  (contains? (-> req :identity :roles) :admin))

(def authz-rules [{:pattern #"^/auth/.*" :handler any?}     ; Let everyone use the auth endpoints
                  {:pattern #"^/admin"
                   :handler is-admin?
                   :on-error (fn [req error]
                               (tmpl/tpl-resp "errors/403-forbidden.html"))}
                  {:pattern  #"^/.*" :handler authenticated?}])

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