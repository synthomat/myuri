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
  [{:keys [identity] :as req}]
  (let [path-roles (-> req :reitit.core/match :data :roles)
        user-roles (:roles identity)]
    (prn path-roles)
    (some? (not-empty (clojure.set/intersection path-roles user-roles)))))

(defn any-access
  "Allows any user"
  [_]
  true)

(defn authenticated-access
  "docstring"
  [req]
  (if (authenticated? req)
    (baa/success)
    (baa/error {:code 401
                :message "You are not authenticated. Please log in."})))

(defn admin-access
  "docstring"
  [{:keys [identity] :as req}]

  (if (contains? (:roles identity) :admin)
    (baa/success)
    (baa/error {:code    403
                :message "Unauthorized admin access"})))

(def rules [{:pattern #"^/auth"
             :handler any-access}
            {:pattern #"^/admin"
             :handler admin-access}
            {:pattern #"^/.*"
             :handler authenticated-access}])

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
  (baa/wrap-access-rules handler {:rules rules}))

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