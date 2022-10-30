(ns myuri.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [com.stuartsierra.component :as component]
            [migratus.core :as migratus]
            [clojure.tools.logging :as log]))


(defn bookmarks
  "docstring"
  [ds user-id]
  (sql/query ds ["select * from bookmarks
                  where user_id = ?
                  order by created_at desc" user-id]))

(defn store!
  "docstring"
  [ds data]
  (sql/insert! ds :bookmarks data))

(defn delete!
  "docstring"
  [ds user-id bookmark-id]
  (log/debug "Deleting bookmark " user-id bookmark-id)
  (sql/delete! ds :bookmarks {:id bookmark-id :user_id user-id}))



(defn user-by-token
  "docstring"
  [ds token]
  (jdbc/execute-one! ds ["select u.* from api_tokens at
                          left join users u on u.id = at.user_id
                          where at.token = ?" token]))

;; Database Management --------------------------------------------------------

(defn migratus-config
  "docstring"
  [ds]
  {:store         :database
   :migration-dir "migrations/"
   :db            {:datasource ds}})

(defrecord DatabaseComponent [options]
  component/Lifecycle

  (start [this]
    (log/info "Starting DatabaseComponent")
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