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

(defn domain-from-url
  "Extracts the domain from a URL.
   If protocol is false it will remove the protocol from the url.
   Fails with empty string if domain can't be parsed"

  ([url protocol]
   (try
     (let [purl (clojure.java.io/as-url url)
           port (when (not= (.getPort purl) -1) (str ":" (.getPort purl)))]
       (str (when protocol (str (.getProtocol purl) "://")) (.getHost purl) port))
     (catch Exception e "")))
  ([url]
   (domain-from-url url false)))

(defn rand-str
  [len]
  (let [chars "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        clen (count chars)]
    (apply str (take len (repeatedly #(get chars (rand clen)))))))