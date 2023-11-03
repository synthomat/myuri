(ns myuri.web.handler
  (:require [bidi.ring :refer [make-handler]]
            [myuri.model :as m]
            [myuri.web.auth.handler :as ah]
            [myuri.web.settings.handler :as sh]
            [myuri.web.bookmarks.handler :as bh]
            [myuri.web.middleware :as mw]
            [myuri.web.utils :as u]
            [myuri.web.views :as v]
            [ring.util.response :as resp]))

;; Utils ----------------------------------------------------------------------

(defn not-found-handler
  "docstring"
  [req]
  (-> (v/layout req
                [:div.container {:style "margin-top: 20px;"}
                 [:h3.is-size-3 {:style "color: #666"} "Page not found"]
                 [:div {:style "color: #888"} "You might have misspelled it or it might just be gone…"]])
      (resp/status 404)))


(defn inject-bookmark
  "Fetches a bookmark by the authenticated user and injects it into the request map"
  [handler]

  (fn [{:keys [ds params] :as req} ]
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
        "backup"           {""    sh/backup-endpoint
                            :post {"/export" sh/export-handler}}
        "auth/"            {"login"    ah/login-handler
                            :post      {"logout" ah/logout}
                            "register" ah/register-handler}
        "settings"         {""        (fn [req] (resp/redirect "/settings/ui"))
                            "/tokens" sh/token-settings-handler
                            "/ui"     sh/ui-settings-handler
                            "/backup" sh/backup-endpoint}

        ;; API ----------------------------------------------------------------
        "api/"             {"bookmarks"       {""        {:get bh/api-index-handler
                                                          :post bh/api-create-bookmark-handler}
                                               ["/" :id] {:get   (-> bh/api-bookmark-handler inject-bookmark)
                                                          :patch (-> bh/api-update-bookmark-handler inject-bookmark)}}
                            "collections"     {"" bh/api-collections-handler}
                            ["user/settings"] {:put {"" sh/config-toggle-handler}}}

        true               not-found-handler}])


(defn new-handler
  [opts]
  (-> (make-handler web-routes)
      (mw/wrap-middlewares opts)))