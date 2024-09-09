(ns myuri.web.routes
  (:require
    [muuntaja.core :as mj]
    [myuri.web.auth.handler :as ah]
    [myuri.web.handler :as bh]
    [myuri.web.middleware :as mw]
    [myuri.web.utils :as u]
    [myuri.api :as api]
    [reitit.ring :as ring]
    [reitit.ring.coercion :as rrc]
    [reitit.ring.middleware.exception :as exception]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.keyword-params :as kpmw]
    [reitit.coercion.malli]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.util.response :as resp]
    [ring.middleware.flash :refer [wrap-flash]]
    [selmer.parser :refer [render-file]]
    [myuri.web.specs :as specs]))

;; Utils ----------------------------------------------------------------------

(defn not-found-handler
  "docstring"
  [req]
  (-> (render-file "errors/error-404.html" {:req req})
      (resp/not-found)))

(defn inject-bookmark
  "Fetches a bookmark by the authenticated user and injects it into the request map"
  [handler]

  (fn [{:keys                 [ds] :as req
        {{:keys [bid]} :path} :parameters}]
    (if-let [bookmark (api/get-bookmark ds (u/user-id req) bid)]
      (handler (assoc req :bookmark bookmark))
      (not-found-handler req))))


(defn make-routes []

  [["/"
    {:get {:parameters {:query specs/GetBookmarksRequest}
           :handler    bh/index-handler}}]
   ["/new"
    {:get  {:parameters {:query [:map
                                 [:data {:optional true} :string]
                                 [:p {:optional true} int?]]}
            :handler    bh/new-bookmark-handler}
     :post {:parameters {:form [:map
                                [:close {:optional true, :default 0} int?]
                                [:url :string]
                                [:title {:optional true} :string]
                                [:description {:optional true} :string]]}
            :handler    bh/new-bookmark-handler}}]
   ["/bookmarks/{bid}"
    {:parameters {:path {:bid uuid?}}
     :middleware [inject-bookmark]}
    [""
     {:delete bh/delete-bookmark-handler}]
    ["/edit" bh/edit-bookmark-handler]]
   ["/auth" {}
    ["/login"
     {:get  {:parameters {:query [:map
                                  [:to {:optional true} string?]]}
             :handler    ah/login-handler-get}
      :post {:parameters {:form {:username string?
                                 :password string?
                                 :to       string?}}
             :handler    ah/login-handler-post}}]
    ["/logout"
     {:post ah/logout-handler}]
    ["/register"
     {:get  {:handler ah/register-handler}
      :post {:parameters {:form {:username string?
                                 :email    string?
                                 :password string?}}
             :handler    ah/register-handler}}]]
   ["/admin" {}
    [""
     {:name    "admin:users"
      :handler bh/admin-users}]]
   ["/settings" {}
    [""
     {:name "settings:general"
      :get  {:handler bh/settings-index}
      :post {:parameters {:form [:map
                                 [:target_blank {:optional true} boolean?]]}
             :handler    bh/settings-index}}]
    ["/security"
     {:name "settings:security"
      :get  {:handler bh/security-handler}
      :post {:handler    bh/security-handler
             :parameters {:form {:current_password string?
                                 :new_password     string?
                                 :new_password2    string?}}}}]]]
  )

(def exception-middleware
  (exception/create-exception-middleware
    (merge exception/default-handlers
           {})))

(defn app
  [opts]
  (ring/ring-handler
    (ring/router
      (make-routes)

      ;; router data affecting all routes
      {:data {:coercion   reitit.coercion.malli/coercion
              :muuntaja   mj/instance
              :middleware [parameters/parameters-middleware
                           kpmw/wrap-keyword-params
                           exception-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware
                           muuntaja/format-response-middleware
                           wrap-anti-forgery
                           mw/wrap-templating
                           mw/wrap-access-rules
                           ]}})

    (ring/routes
      (ring/create-resource-handler {:path "/assets"})
      (ring/create-default-handler {:not-found not-found-handler}))

    {:middleware [[mw/wrap-session (:cookie-secret opts)]
                  wrap-flash

                  mw/wrap-authentication
                  mw/wrap-authorization
                  [mw/wrap-system opts]]}))