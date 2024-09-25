(ns myuri.web.middleware
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :as baa]
            [buddy.auth.backends :as auth-backends]
            [buddy.auth.middleware :as bam]
            [myuri.web.auth.handler :refer [unauthorized-handler]]
            [ring.middleware.session :as rms]
            [ring.middleware.session.cookie :as cookie]
            [myuri.web.templating :as tmpl]))


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
    (baa/error {:code    401
                :message "You are not authenticated. Please log in."})))

(defn admin-access
  "docstring"
  [{:keys [identity] :as req}]

  (if (contains? (:roles identity) :admin)
    (baa/success)
    ;(tmpl/tpl-resp "errors/403-forbidden.html")
    (baa/error (fn [req] (tmpl/tpl-resp "errors/403-forbidden.html")))
    #_{:code    403
       :message "Unauthorized admin access"}))

(def rules [{:uri     "/auth"
             :handler any-access}
            {:uri     "/admin"
             :handler admin-access}
            {:uri     "/"
             :handler authenticated-access}])

(defn wrap-session
  "docstring"
  [handler key]
  (let [store (cookie/cookie-store {:key (.getBytes key)})]
    (rms/wrap-session handler {:store store})))

(def auth-backend (auth-backends/session
                    {:unauthorized-handler unauthorized-handler}))

(defn wrap-authentication
  "docstring"
  [handler]
  (bam/wrap-authentication handler auth-backend))

(defn wrap-authorization
  "docstring"
  [handler]
  (bam/wrap-authorization handler auth-backend))

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


(defn wrap-templating
  "docstring"
  [handler]
  (tmpl/wrap-template-response handler))