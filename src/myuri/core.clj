(ns myuri.core
  (:require [com.stuartsierra.component :as comp]
            [myuri.system :as system]
            [aero.core :refer [read-config]]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn -main
  [& args]
  (let [config (read-config (clojure.java.io/resource "config.edn"))
        system (system/new-system config)]
    (log/info "Starting System")
    (comp/start-system system)))