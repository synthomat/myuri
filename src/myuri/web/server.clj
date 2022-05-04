(ns myuri.web.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as j]
            [myuri.web.handler :as handler]))

(defrecord ServerComponent [options db]
  component/Lifecycle

  (start [this]
    (println ";; Starting ServerComponent")
    (let [{:keys [port]} options
          handler (handler/new-handler {:ds (:ds db)})
          server (j/run-jetty handler {:port port :join? false})]
      (assoc this :server server)))

  (stop [this]
    (when-let [server (:server this)]
      (.stop server))

    (assoc this :server nil)))


(defn new-server [opts]
  (map->ServerComponent {:options opts}))