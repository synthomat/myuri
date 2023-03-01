(ns myuri.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [com.stuartsierra.component :as component]
            [migratus.core :as migratus]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [next.jdbc.prepare :as prepare]
            [next.jdbc.result-set :as rs]))

(import '(org.postgresql.util PGobject)
        '(java.sql PreparedStatement))


(def ->json json/generate-string)
(def <-json #(json/parse-string % true))

(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
  [^PGobject v]
  (let [type (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (when value
        (<-json value))
      value)))

(set! *warn-on-reflection* true)

;; if a SQL parameter is a Clojure hash map or vector, it'll be transformed
;; to a PGobject for JSON/JSONB:
(extend-protocol prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))

  clojure.lang.IPersistentVector
  (set-parameter [v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))

;; if a row contains a PGobject then we'll convert them to Clojure data
;; while reading (if column is either "json" or "jsonb" type):
(extend-protocol rs/ReadableColumn
  PGobject
  (read-column-by-label [^PGobject v _] (<-pgobject v))
  (read-column-by-index [^PGobject v _2 _3] (<-pgobject v)))

;; –––

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