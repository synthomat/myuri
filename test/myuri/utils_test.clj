(ns myuri.utils-test
  (:require [clojure.test :refer :all]
            [myuri.web.utils :as u]))


(deftest is-post-test
  (testing "Should detect POST requests"
    (is (true? (u/is-post? {:request-method :post})))
    (is (false? (u/is-post? {:request-method :get})))))

(deftest web-app-address-extraction
  (testing "Should return app address"
    (is (= "https://example.com:443"
           (u/app-address {:scheme      "https"
                           :server-name "example.com"
                           :server-port 443})))))