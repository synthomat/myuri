(ns myuri.web.handler
  (:require [bidi.ring :refer [make-handler]]
            [myuri.web.auth.handler :as ah]
            [myuri.web.settings.handler :as sh]
            [myuri.web.bookmarks.handler :as bh]
            [myuri.web.middleware :as mw]
            [myuri.web.views :as v]
            [ring.util.response :as resp]))

;; Utils ----------------------------------------------------------------------

(defn not-found-handler
  "docstring"
  [req]
  (-> (v/layout req
                [:div.container {:style "margin-top: 20px;"}
                 [:h3.is-size-3 {:style "color: red"} "Page not found"]])
      (resp/status 404)))


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
        "api/"             {"bookmarks"       {"" (fn [req]
                                                    (resp/response
                                                      {:response "ok"}))}
                            ["user/settings"] {:put {"" sh/config-toggle-handler}}}

        true               not-found-handler}])


(defn new-handler
  [opts]
  (-> (make-handler web-routes)
      (mw/wrap-middlewares opts)))