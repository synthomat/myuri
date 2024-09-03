(ns myuri.web.server
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [myuri.web.routes :as routes]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as j]))


(defrecord ServerComponent [options db]
  component/Lifecycle

  (start [this]
    (log/info "Starting ServerComponent")

    (selmer.parser/set-resource-path! (clojure.java.io/resource "templates"))
    (selmer.parser/cache-off!)

    (let [{:keys [cookie-secret port dev?]} options

          create-handler #(routes/app {:ds            (:ds db)
                                       :cookie-secret cookie-secret})

          handler (if dev?
                    (do
                      (log/info "Starting server in debug mode (auto-reload enabled)")
                      (ring/reloading-ring-handler create-handler))
                    (create-handler))

          server (j/run-jetty handler {:port port :join? false})]
      (assoc this :server server)))

  (stop [this]
    (when-let [server (:server this)]
      (.stop server))

    (assoc this :server nil)))


(defn new-server [opts]
  (map->ServerComponent {:options opts}))