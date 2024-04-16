(ns myuri.core
  (:gen-class)
  (:require [com.stuartsierra.component :as comp]
            [myuri.system :as system]
            [aero.core :as aero]
            [clojure.tools.logging :as log]))


(defn read-config
  "docstring"
  [file-name]
  (aero/read-config (clojure.java.io/resource file-name)))


(defn -main
  [& args]
  (let [config (read-config "config.defaults.edn")
        system (system/new-system config)]
    (log/info "Starting System")
    (comp/start-system system)))