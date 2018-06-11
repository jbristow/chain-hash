(ns chain-hash.util-test
  (:require [chain-hash.util :refer :all]
            [clojure.test :refer [deftest is testing]])
  (:import (java.util Arrays)))

(deftest hexify-test
  (testing "handles empty"
    (is (= (hexify (byte-array [])) "")))
  (testing "2 digit numbers"
    (is (= (hexify (byte-array [0xff 20])) "ff14")))
  (testing "1 digit numbers"
    (is (= (hexify (byte-array [1 2])) "0102")))
  (testing "left-pad"
    (is (= (hexify (byte-array [1 0xff]) 10)) "00000001ff")))

(deftest unhexify-test
  (testing "identity"
    (let [test-data (.getBytes "12345 Put that on my luggage!")]
      (is (Arrays/equals (unhexify (hexify test-data)) test-data))))
  (testing "inverse identity"
    (is (= "010203" (hexify (unhexify "010203")))))

  (testing "handles empty"
    (is (Arrays/equals (byte-array []) (unhexify "")))))

(deftest sha256-test
  (testing "handles empty"
    (is (= "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
           (hexify (sha256 (.getBytes ""))))))
  (testing "happy-path"
    (is (= "c876cd356012b45874f9e845038b763b4e4d99f9b91450c3840f1e9d3adace64"
           (hexify (sha256 (.getBytes "testing, 1 2 3")))))))

(deftest base64-encode-test
  (testing "handles empty"
    (is (= "" (base64-encode (byte-array [])))))
  (testing "happy-path"
    (is (= "SGVsbG8gbXkgYmFieSwgaGVsbG8gbXkgaG9uZXku"
           (base64-encode (.getBytes "Hello my baby, hello my honey."))))))
