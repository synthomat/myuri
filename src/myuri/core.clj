(ns myuri.core
  (:gen-class)
  (:require [com.stuartsierra.component :as comp]
            [myuri.system :as system]
            [aero.core :as aero]
            [clojure.tools.logging :as log]))

(defn -main
  [& args]
  (let [config (aero/read-config (clojure.java.io/resource "config.defaults.edn"))
        system (system/new-system config)]
    (log/info "Starting System")
    (comp/start-system system)))