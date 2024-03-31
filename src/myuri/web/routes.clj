(ns myuri.web.routes
  (:require [bidi.ring :refer [make-handler]]
            [myuri.model :as m]
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

  (fn [{:keys [ds params] :as req}]
    (let [user-id (u/user-id req)
          bm-id (-> params :id parse-uuid)]

      (if-let [bookmark (m/bookmark-by-id ds user-id bm-id)]
        (handler (assoc req :bookmark bookmark))
        (-> (resp/not-found {:message (format "Could not find bookmark: %s" (-> params :id))})
            (resp/content-type "application/json"))))))

;; Routes and Middlewares -----------------------------------------------------
(def web-routes
  ["/" {""                 bh/index-handler
        "new"              bh/new-bookmark-handler
        ["bookmarks/" :id] {:delete {"" bh/delete-bookmark-handler}
                            "/edit" bh/edit-bookmark-handler}
        "auth/"            {"login"    ah/login-handler
                            :post      {"logout" ah/logout}
                            "register" ah/register-handler}

        true               not-found-handler}])


(defn new-handler
  [opts]
  (-> (make-handler web-routes)
      (mw/wrap-middlewares opts)))