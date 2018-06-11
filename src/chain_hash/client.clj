(ns chain-hash.client
  (:require [chain-hash.file :as f]
            [chain-hash.server :as server]
            [chain-hash.util :as util])
  (:import (java.util Arrays))
  (:gen-class))

(defn un-merge-data [arr size]
  (let [len-a size
        len-b (- (count arr) size)
        a (byte-array len-a)
        b (byte-array len-b)]
    (System/arraycopy arr 0 a 0 len-a)
    (System/arraycopy arr len-a b 0 len-b)
    {:data a :hash b}))

(defn piece-valid? [piece ^String checksum]
  (Arrays/equals (util/sha256 piece) (util/unhexify checksum)))

(defn fetch-piece [file piece size]
  (let [max-piece (server/count-pieces file size)]
    (if (< 0 piece max-piece)
      (util/base64-encode (server/fetch-piece file piece size))
      (throw (ex-info "Illegal piece requested." {:requested piece
                                                  :acceptable-range [0 max-piece]
                                                  :filename file})))))

(defn fetch
  "Fetch all pieces of a file from the server.

  For now, imagine that server/fetch-piece is a call to a REST api."
  [file checksum size output-handler]

  (loop [n 1
         curr-checksum checksum]
    (let [curr-piece (server/fetch-piece file n size)]
      (if (piece-valid? curr-piece curr-checksum)
        (do (println "piece" n "downloaded and valid.")
            (if (= (count curr-piece) (+ size 32))
              (let [{h :hash d :data} (un-merge-data curr-piece size)]
                (output-handler d)
                (recur (inc n) (util/hexify h)))
              (output-handler curr-piece)))
        (throw (ex-info "Checksum error" {:piece n
                                          :invalid-checksum curr-checksum
                                          :actual (util/hexify
                                                   (util/sha256 curr-piece))}))))))
(defn count-pieces
  "Simulate the client calling the server to ask for the number of pieces."
  [filename size]
  (server/count-pieces filename size))

(defn file-append-handler
  "Default file-handler that just appends data to a given file."
  [filename]
  (fn [data] (f/append-to filename data)))
