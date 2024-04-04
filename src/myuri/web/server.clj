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
    (let [{:keys [cookie-secret port]} options
          create-handler #(routes/app {:ds            (:ds db)
                                       :cookie-secret cookie-secret})
          server (j/run-jetty (ring/reloading-ring-handler create-handler)
                              {:port port :join? false})]
      (assoc this :server server)))

  (stop [this]
    (when-let [server (:server this)]
      (.stop server))

    (assoc this :server nil)))


(defn new-server [opts]
  (map->ServerComponent {:options opts}))