(ns chain-hash.server
  (:require [chain-hash.file :as f :refer [read-byte-array]]
            [chain-hash.util :as u]
            [clojure.java.io :as io])
  (:import (java.nio.charset StandardCharsets)
           (java.nio.file Files))
  (:gen-class))

(def hash-extension ".shashes")

(defn fetch-hash
  "Fetch hash n from the .shashes file for filename"
  [filename piece]
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

(defn count-pieces
  "return the number of pieces filename will be split into"
  [filename size]
  (int (inc (/ (f/file-size filename) size))))

(defn fetch-piece
  "Return piece n to the client."
  [file piece size]
  (let [hashv (u/unhexify (fetch-hash file piece))
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
  (let [final-hash (u/sha256 last-data)]
    (reductions (fn [prev-hash curr-data]
                  (u/sha256 (concat-byte-array curr-data prev-hash)))
                final-hash rest-data)))

(defn save-hashes-to-file
  "Write a list of binary-arrays as hex strings to `filename.shashes`."
  [hashes filename]
  (with-open [wrtr (io/writer (str filename hash-extension))]
    (doseq [h hashes]
      (.write wrtr (str (u/hexify h 64) "\n")))))

(defn merge-file
  "merge hashes with original data"
  [hashes infile outfile size]
  (let [reader (f/byte-channel infile)
        total-size (f/file-size infile)]
    (with-open [wrtr (f/output-stream-appender outfile)]
      (doseq [[i h] (map-indexed vector (concat (rest hashes) [(byte-array 0)]))]
        (let [startpos (* i size)
              readsize (min size (- total-size (* i size)))
              indata (read-byte-array reader startpos readsize)]
          (.write wrtr indata)
          (.write wrtr h))))
    (println (u/hexify (first hashes)))))

(defn generate-hashes-for
  "Take a file (infile), generate the hashes, and then save them to a .shashes file"
  [file size]
  (-> file
      (data-chunks-back-to-front size)
      hash-sequence
      reverse
      (save-hashes-to-file file)))

(defn encode
  "Takes a file (infile) and generates the hashes before stitching a new file
   (outfile) back together, with the hashes included as binary in the new file."
  [infile outfile size]
  (-> infile
      (data-chunks-back-to-front size)
      hash-sequence
      reverse
      (merge-file infile outfile size)))
