(ns myuri.web.utils)

(defn user-id
  "Extracts the user-id from the session"
  [req]
  (-> req :identity :id))

(defn parse-int [s]
  (Integer. (re-find #"[0-9]*" s)))