(ns myuri.web.routes
  (:require
    [muuntaja.core :as mj]
    [myuri.model :as m]
    [myuri.web.auth.handler :as ah]
    [myuri.web.handler :as bh]
    [myuri.web.middleware :as mw]
    [myuri.web.utils :as u]

    [reitit.ring :as ring]
    [reitit.ring.coercion :as rrc]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.util.response :as resp]
    [reitit.coercion.malli]
    [selmer.parser :refer [render-file]]
    [myuri.web.templating :refer [tpl-resp]]))

;; Utils ----------------------------------------------------------------------

(defn not-found-handler
  "docstring"
  [req]
  (-> (render-file "error-404.html" {:req req})
      (resp/not-found)))


(defn inject-bookmark
  "Fetches a bookmark by the authenticated user and injects it into the request map"
  [handler]

  (fn [{:keys [ds] :as req}]
    (let [user-id (u/user-id req)
          bm-id (-> req :parameters :path :bid)]

      (if-let [bookmark (m/bookmark-by-id ds user-id bm-id)]
        (handler (assoc req :bookmark bookmark))
        (not-found-handler req)))))

(def default-routes
  (ring/routes
    (ring/create-resource-handler {:path "/assets"})
    (ring/create-default-handler {:not-found not-found-handler})))

(defn app
  [opts]
  (ring/ring-handler
    (ring/router
      [["/" {:get {:parameters {:query [:map [:q {:optional true} string?]]}
                   :handler    bh/index-handler}}]
       ["/new" bh/new-bookmark-handler]
       ["/bookmarks/{bid}" {:parameters {:path {:bid uuid?}}
                            :middleware [inject-bookmark]}
        ["" {:delete bh/delete-bookmark-handler}]
        ["/edit" bh/edit-bookmark-handler]]
       ["/auth" {}
        ["/login" {:get  {:parameters {:query [:map [:to {:optional true} string?]]}
                          :handler    ah/login-handler-get}
                   :post {:parameters {:form {:username string?
                                              :password string?
                                              :to       string?}}
                          :handler    ah/login-handler-post}}]
        ["/logout" {:post ah/logout-handler}]
        ["/register" ah/register-handler]]]

      ;; router data affecting all routes
      {:data {:coercion   reitit.coercion.malli/coercion
              :muuntaja   mj/instance
              :middleware [parameters/parameters-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware
                           muuntaja/format-response-middleware
                           ;exception/exception-middleware
                           wrap-anti-forgery

                           mw/wrap-templating]}})

    default-routes

    {:middleware [[mw/wrap-session (:cookie-secret opts)]

                  mw/wrap-authentication
                  mw/wrap-authorization
                  mw/wrap-access-rules

                  [mw/wrap-system opts]]}))