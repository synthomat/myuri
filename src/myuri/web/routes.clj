(ns myuri.web.routes
  (:require
    [reitit.ring :as ring]
    [muuntaja.core :as mj]
    [reitit.ring.coercion :as rrc]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [ring.middleware.session :refer [wrap-session]]
    [reitit.ring.middleware.parameters :as parameters]
    [myuri.model :as m]
    [reitit.coercion.malli]
    [myuri.web.auth.handler :as ah]
    [myuri.web.handler :as bh]
    [myuri.web.middleware :as mw]
    [myuri.web.utils :as u]
    [selmer.parser :refer [render-file]]

    [ring.util.response :as resp]))

;; Utils ----------------------------------------------------------------------

(defn not-found-handler
  "docstring"
  [req]
  (-> (render-file "error-404.html" {:req req})
      (ring.util.response/not-found)
      (ring.util.response/content-type "text/html")))


(defn inject-bookmark
  "Fetches a bookmark by the authenticated user and injects it into the request map"
  [handler]

  (fn [{:keys [ds] :as req}]
    (let [user-id (u/user-id req)
          bm-id (-> req :parameters :path :bid)]

      (if-let [bookmark (m/bookmark-by-id ds user-id bm-id)]
        (handler (assoc req :bookmark bookmark))
        (-> (resp/not-found {:message (format "Could not find bookmark: %s" bm-id)}))))))

(defn app
  [opts]
  (ring/ring-handler
    (ring/router
      [["/" {}
        ["" bh/index-handler]
        ["new" bh/new-bookmark-handler]
        ["bookmarks/{bid}" {:parameters {:path {:bid uuid?}}
                            :middleware [inject-bookmark]}
         ["" {:delete bh/delete-bookmark-handler}]
         ["/edit" bh/edit-bookmark-handler]]]
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
              :middleware [mw/wrap-authentication
                           mw/wrap-authorization
                           mw/wrap-access-rules

                           parameters/parameters-middleware
                           rrc/coerce-request-middleware
                           muuntaja/format-response-middleware
                           rrc/coerce-response-middleware

                           mw/wrap-template-response]}})

    (ring/routes
      (ring/create-resource-handler {:path "/assets"})
      (ring/create-default-handler))

    {:middleware [[mw/wrap-session (:cookie-secret opts)]
                  [mw/wrap-system opts]]}))


(defn new-handler
  [opts]
  (app opts))