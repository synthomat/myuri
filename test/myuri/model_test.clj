(ns myuri.model-test
  (:require [clojure.test :refer :all]
            [myuri.web.auth.handler :as ah]))

(deftest user-model-validation
  (let [valid? #(nil? (ah/validate-model ah/User-Registration %))
        validate #(ah/validate-model ah/User-Registration %)]
    (testing "Check valid User Model"
      (is (nil? (validate {:username "abc"
                         :email    "abc@example.com"
                         :password "abcdefghijklmn"}))))
    (testing "Check empty data"
      (is (some? (validate {}))))))