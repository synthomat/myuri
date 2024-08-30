(ns myuri.web.render
  (:require [myuri.web.templating :refer [tpl-resp]]
            [ring.util.response :as resp]))



(defn index
  "docstring"
  [resp]

  (tpl-resp "index.html" resp))