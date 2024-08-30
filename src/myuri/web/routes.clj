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
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.keyword-params :as kpmw]
    [reitit.coercion.malli]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.util.response :as resp]
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

(def default-routes
  (ring/routes
    (ring/create-resource-handler {:path "/assets"})
    (ring/create-default-handler {:not-found not-found-handler})))

(defn app
  [opts]
  (ring/ring-handler
    (ring/router
      [["/" {:get {:parameters {:query specs/GetBookmarksRequest}
                   :handler    bh/index-handler}}]
       ["/new" {:get  {:parameters {:query [:map
                                            [:data {:optional true} :string]
                                            [:p {:optional true} int?]]}
                       :handler    bh/new-bookmark-handler}
                :post {:parameters {:form [:map
                                           [:close {:optional true, :default 0} int?]
                                           [:url :string]
                                           [:title {:optional true} :string]
                                           [:description {:optional true} :string]]}
                       :handler    bh/new-bookmark-handler}}]
       ["/bookmarks/{bid}" {:parameters {:path {:bid uuid?}}
                            :middleware [inject-bookmark]}
        ["" {:delete bh/delete-bookmark-handler}]
        ["/edit" bh/edit-bookmark-handler]]
       ["/auth" {}
        ["/login" {:get  {:parameters {:query [:map
                                               [:to {:optional true} string?]]}
                          :handler    ah/login-handler-get}
                   :post {:parameters {:form {:username string?
                                              :password string?
                                              :to       string?}}
                          :handler    ah/login-handler-post}}]
        ["/logout" {:post ah/logout-handler}]
        ["/register" {:get  {:handler ah/register-handler}
                      :post {:parameters {:form {:username string?
                                                 :email    string?
                                                 :password string?}}
                             :handler    ah/register-handler}}]]
       ["/settings" {}
        ["" {:get  {:name    "settings:general"
                    :handler bh/settings-index}
             :post {:name    "settings:general:post"
                    :parameters {:form [:map
                                        [:target_blank {:optional true} boolean?]]}
                    :handler bh/settings-index}}]
        ["/security" {:name    "settings:security"
                      :handler bh/security-handler}]]
       ]

      ;; router data affecting all routes
      {:data {:coercion   reitit.coercion.malli/coercion
              :muuntaja   mj/instance
              :middleware [parameters/parameters-middleware
                           kpmw/wrap-keyword-params
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware
                           muuntaja/format-response-middleware
                           ;exception/exception-middleware
                           [wrap-anti-forgery]

                           mw/wrap-templating]}})

    default-routes

    {:middleware [[mw/wrap-session (:cookie-secret opts)]

                  mw/wrap-authentication
                  mw/wrap-authorization
                  mw/wrap-access-rules

                  [mw/wrap-system opts]]}))