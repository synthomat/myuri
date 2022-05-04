(ns myuri.core
  (:require [com.stuartsierra.component :as comp]
            [myuri.system :as system]
            [aero.core :refer [read-config]])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [config (read-config (clojure.java.io/resource "config.edn"))
        system (system/new-system config)]

    (comp/start-system system)))