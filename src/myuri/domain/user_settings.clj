(ns myuri.domain.user-settings)


(def setting-defaults
  {:display_icons   false
   :detail_fetching true
   :on_duplicate    :update_created_at                      ; :throw-error / :do-nothing
   })

(defn valid-setting?
  "docstring"
  ([setting-name]
   (valid-setting? setting-defaults setting-name))

  ([valid-settings setting-name]
   (contains? valid-settings setting-name)))


(defn add-defaults
  "docstring"
  [defaults settings]
  settings)

(defn get-user-settings
  "docstring"
  ([getter-fn user-id snames]
   (->> (getter-fn user-id snames)
        (add-defaults setting-defaults)))
  ([getter-fn user-id]
   (get-user-settings getter-fn user-id nil)))

(defn get-user-setting
  "docstring"
  [getter-fn user-id sname]
  (get-user-settings getter-fn user-id [sname]))


(defn update-user-setting!
  "docstring"
  [writer-fn user-id sname svalue]

  (when (not (valid-setting? sname))
    (throw (Exception. (format "[%s] is not a valid property" sname))))

  (writer-fn user-id sname svalue))