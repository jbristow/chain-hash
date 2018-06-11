(ns chain-hash.server
  (:require [chain-hash.file :as f]
            [chain-hash.util :as util]
            [clojure.java.io :as io])
  (:import (java.nio.charset StandardCharsets)
           (java.nio.file Files))
  (:gen-class))

(def hash-extension ".shashes")

(defn fetch-hash [filename piece]
  (let [file-path (f/path (str filename hash-extension))
        reader (if (Files/isReadable file-path)
                 (Files/newBufferedReader file-path StandardCharsets/UTF_8)
                 (throw (ex-info "Hash file not found." {:filename (str file-path)})))
        lines (line-seq reader)]
    (first (drop piece lines))))

(defn concat-byte-array
  "Slightly speed-optimized byte-array manipulation to avoid the overhead of
   converting to and from clojure seq."
  [a b]
  (let [new-array (byte-array (+ (count a) (count b)))]
    (System/arraycopy a 0 new-array 0 (count a))
    (System/arraycopy b 0 new-array (count a) (count b))
    new-array))

(defn count-pieces [filename size]
  (int (inc (/ (f/file-size filename) size))))

(defn fetch-piece [file piece size]
  (let [hashv (util/unhexify (fetch-hash file piece))
        bytecount (f/file-size file)
        data (f/read-byte-array (f/byte-channel file)
                                (* size (dec piece))
                                (if (> (* size piece) bytecount)
                                  (mod bytecount size) size))]

    (if (> (* size piece) bytecount)
      data
      (concat-byte-array data hashv))))

(defn calculate-data-chunk-info
  "Return a list of byte locations and the expected size of each resulting
   bytestream. They will all be identical except for the last one unless the
   file size is a multiple of 2^10"
  [filename size]
  (let [total-size (f/file-size filename)]
    (map #(vector % (min size (- total-size %))) (range 0 total-size size))))

(defn data-chunks-back-to-front
  "Lazy-load binary data chunks from the end back to the front of the file."
  [filename size]
  (let [positions (calculate-data-chunk-info filename size)
        channel (f/byte-channel filename)]
    (map (fn [[p s]] (f/read-byte-array channel p s)) (reverse positions))))

(defn hash-sequence
  "Takes a sequence of binary data and returns a list of the resulting
   sequential hashes. This assumes the data is coming in from last to first and
   does NOT change the output order. The last hash is still in front."
  [[last-data & rest-data :as data]]
  (let [final-hash (util/sha256 last-data)]
    (reductions (fn [prev-hash curr-data]
                  (util/sha256 (concat-byte-array curr-data prev-hash)))
                final-hash rest-data)))

(defn save-hashes-to-file
  "Write a list of binary-arrays as hex strings to `filename.shashes`."
  [hashes filename]
  (with-open [wrtr (io/writer (str filename hash-extension))]
    (doseq [h hashes]
      (.write wrtr (str (util/hexify h 64) "\n")))))

(defn generate-hashes-for [file size]
  (-> file
      (data-chunks-back-to-front size)
      hash-sequence
      reverse
      (save-hashes-to-file file)))
