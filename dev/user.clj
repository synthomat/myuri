(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh]]
            [myuri.system :as system]
            [aero.core :refer [read-config]]
            [migratus.core :as migratus]))

(def system nil)

(defn init []
  (let [config (read-config (clojure.java.io/resource "config.defaults.edn"))]
    (alter-var-root #'system
                    (constantly (system/new-system config)))))

(defn start []
  (alter-var-root #'system component/start)
  nil)

(defn stop []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn new-migration
  "docstring"
  [name]
  (migratus/create (-> system :db :migratus) name)  )