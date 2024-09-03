(ns myuri.link-checker
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]))


(defrecord LinkChecker [db]
  component/Lifecycle
  (start [this]
    (log/info "Starting LinkChecker")
    this
    )

  (stop [this]
    (log/info "Stopping LinkChecker")
    this
    ))


(defn new-linkchecker
  "docstring"
  []
  (map->LinkChecker {}))