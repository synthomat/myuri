(ns myuri.system
  (:require [myuri.db :as db]
            [myuri.web.server :as server]
            [com.stuartsierra.component :as component]))

(defn new-system [config]
  (let [{:keys [database server]} config]
    (component/system-map
      :db (db/new-database database)
      :server (component/using (server/new-server server)
                               [:db]))))
