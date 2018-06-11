(ns chain-hash.server-test
  (:require [chain-hash.server :refer :all]
            [chain-hash.util :as u]
            [clojure.test :refer [deftest is testing]])
  (:import (java.util Arrays)))

(deftest concat-byte-array-test
  (testing "adding two byte arrays"
    (let [a (.getBytes "hello")
          b (.getBytes "hello world")
          c (concat-byte-array a b)]
      ; I'm exploiting java's String->ByteArray specifics here
      (is (Arrays/equals (.getBytes "hellohello world") c))
      (is (= (+ (count a) (count b)) (count c)))))

  (testing "adding two empty"
    (let [a (byte-array 0)
          b (byte-array 0)
          c (concat-byte-array a b)]
      (is (Arrays/equals (byte-array 0) c))
      (is (zero? (count c)))))
  (testing "adding two byte arrays, first empty"
    (let [a (byte-array 0)
          b (.getBytes "hello")
          c (concat-byte-array a b)]
      (is (Arrays/equals (.getBytes "hello") c))
      (is (= (count b) (count c)))))

  (testing "adding two byte arrays, second empty"
    (let [a (.getBytes "hello")
          b (byte-array 0)
          c (concat-byte-array a b)]
      (is (Arrays/equals (.getBytes "hello") c))
      (is (= (count a) (count c))))))

(deftest hash-sequence-test
  (testing "two chunk test"
    (let [[da db :as data] [(byte-array [1 2 3 4 5]) (byte-array [6 7 8 9 10])]
          [ha hb] (hash-sequence data)]
      (is (Arrays/equals hb (u/sha256 (concat-byte-array db ha))))
      (is (Arrays/equals ha (u/sha256 da)))))

  (testing "three chunk test"
    (is (= ["74f81fe167d99b4cb41d6d0ccda82278caee9f3e2f25d5e5a3936ff3dcec60d0"  "92666bf4741a8ace4ed302e06290c926ca3ac056ae987acd0d3d9d7c5fd79cce"  "c9c60d710fbd4ccbd4ccebc732dafb23dbea22e9c299e06bd70834ad3d8445ae"]
           (map u/hexify (hash-sequence [(byte-array [1 2 3 4 5])
                                         (byte-array [6 7 8 9 10])
                                         (byte-array [11 12 13 14 15])]))))))

(deftest count-pieces-test
  (testing "count-pieces: dangerous: filesystem hit, relies on test data"
    (is (= 12 (count-pieces "resources/qrcode.png" 1024)))))
