(ns myuri.web.specs)

(def CreateBookmarkRequest
  [:map
   [:url  [:string {:min 12 :max 1024}]]
   [:title {:optional true} [:string {:min 2 :max 1024}]]
   [:tags {:optional true} [:vector :string]]])

(def GetBookmarksRequest
  [:map
   [:q {:optional true} string?]])