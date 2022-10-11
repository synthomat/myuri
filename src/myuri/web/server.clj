(ns myuri.web.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as j]
            [myuri.web.handler :as handler]
            [clojure.tools.logging :as log]))

(defrecord ServerComponent [options db]
  component/Lifecycle

  (start [this]
    (log/info "Starting ServerComponent")
    (let [{:keys [cookie-secret port]} options
          handler (handler/new-handler {:ds (:ds db)
                                        :cookie-secret cookie-secret})
          server (j/run-jetty handler {:port port :join? false})]
      (assoc this :server server)))

  (stop [this]
    (when-let [server (:server this)]
      (.stop server))

    (assoc this :server nil)))


(defn new-server [opts]
  (map->ServerComponent {:options opts}))