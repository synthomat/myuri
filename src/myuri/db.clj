(ns myuri.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [com.stuartsierra.component :as component]
            [migratus.core :as migratus]))


(defn bookmarks
  "docstring"
  [ds]
  (sql/query ds ["select * from bookmarks
                 order by created_at desc"]))

(defn store!
  "docstring"
  [ds data]
  (sql/insert! ds :bookmarks data))

(defn delete!
  "docstring"
  [ds bookmark-id]
  (sql/delete! ds :bookmarks {:id bookmark-id}))

(defn migratus-config
  "docstring"
  [ds]
  {:store         :database
   :migration-dir "migrations/"
   :db            {:datasource ds}})

(defrecord DatabaseComponent [options]
  component/Lifecycle

  (start [this]
    (println ";; Starting DatabaseComponent")
    (let [{:keys [url]} options
          db-spec {:jdbcUrl url}
          ds (jdbc/get-datasource db-spec)
          migratus-conf (migratus-config ds)]

      (migratus/migrate migratus-conf)

      (assoc this :ds ds
                  :migratus migratus-conf)))

  (stop [this]
    (assoc this :ds nil)))

(defn new-database [opts]
  (map->DatabaseComponent {:options opts}))