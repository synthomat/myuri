(ns myuri.web.utils)

(defn user-id
  "Extracts the user-id from the session"
  [req]
  (-> req :identity :id))

(defn parse-int [s]
  (Integer. (re-find #"[0-9]*" s)))

(defn is-post?
  "docstring"
  [req]
  (= (-> req :request-method) :post))

(defn app-address
  "docstring"
  [req]
  (str (-> req :scheme name) "://" (:server-name req) ":" (:server-port req)))