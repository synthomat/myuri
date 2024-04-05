(ns myuri.web.templating
  (:require [myuri.web.utils :as u]
            [ring.util.response :as resp]
            [selmer.parser :refer [render-file]]))


(defn tpl-resp
  "docstring"
  ([template] (tpl-resp template nil))
  ([template data]
   (resp/response
     {:selmer {:template template
               :data     data}})))

(defn tpl-resp?
  "docstring"
  [resp]
  (-> resp :body :selmer))

(defn wrap-template-response
  "docstring"
  [handler]
  (fn [req]
    (let [res (handler req)]
      (if-let [selm (tpl-resp? res)]
        (let [{:keys [template data]} selm
              tpl-data (merge {:app-addr (u/app-address req)
                               :req      req}
                              data)]
          (assoc res :body (render-file template tpl-data)))
        res))))