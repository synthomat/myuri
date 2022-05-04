(ns build
  (:refer-clojure :exclude [test])
  (:require [org.corfield.build :as bb]))

(def lib 'net.clojars.synthomat/myuri)
(def version "0.1.0-SNAPSHOT")
(def main 'myuri.core)

(defn test "Run the tests." [opts]
  (bb/run-tests opts))

(defn uber "Run the CI pipeline of tests (and build the uberjar)." [opts]
  (-> opts
      (assoc :lib lib :version version :main main)
      (bb/clean)
      (bb/uber)))