(ns chain-hash.core-test
  (:require [chain-hash.core :refer :all]
            [clojure.test :refer [deftest is testing]])
  (:import (clojure.lang ExceptionInfo)))

(deftest exception-test
  (testing "basic happy path"
    (let [err (missing-required-argument :arbitrary-keyword)]
      (is (= (.getMessage err) "Missing required argument"))
      (is (= (ex-data err) {:missing-argument "arbitrary-keyword"})))))

(deftest validate-has-option-test
  (testing "no options - no list - pass"
    (is (= (validate-has-option {} [] "pass") "pass")))
  (testing "no options - some list - fail"
    (is (thrown-with-msg? ExceptionInfo #"Missing required argument"
                          (validate-has-option {} [:o1] "pass"))))
  (testing "some options - some list - fail"
    (is (thrown-with-msg? ExceptionInfo #"Missing required argument"
                          (validate-has-option {:o1 "present" :o2 "present"} [:o1 :o2 :o3] "pass"))))
  (testing "some options - some list - pass"
    (is (= (validate-has-option {:o1 "present"} [:o1] "pass") "pass"))))
