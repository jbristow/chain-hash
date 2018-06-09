(ns chain-hash.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io File RandomAccessFile)
           (java.nio.file Files Paths)
           (java.security MessageDigest))
  (:gen-class))

(defn -main
  "TODO: MAKE INTO REAL (fake) CLI"
  [& args]
  (println "Hello, World!"))

(defn hexify
  "Returns the hex-string of a ByteArray. If a minimum length is added, the
   resulting hex will be left-padded with zeros.

   Based on https://stackoverflow.com/questions/10062967/clojures-equivalent-to-pythons-encodehex-and-decodehex#10065003"
  ([bs]
   (hexify bs 0))
  ([bs min-len]
   (let [raw (map #(format (str "%02x") (bit-and 0xff %)) bs)]
     (str/join "" (concat (repeat (max 0 (- min-len (* 2 (count raw)))) "0") raw)))))

(defn sha256
  "Returns the hash of a ByteArray as a new ByteArray.

   Based on https://gist.github.com/kubek2k/8446062"
  [bs]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (.digest md bs)))

(defn calculate-data-chunk-info
  "Return a list of byte locations and the expected size of each resulting
   bytestream. They will all be identical except for the last one unless the
   file size is a multiple of 2^10"
  [filename]
  (let [path (Paths/get filename (into-array [""]))
        total-size (Files/size path)]
    (map #(vector % (min 1024 (- total-size % 1))) (range 0 total-size 1024))))

(defn get-byte-array
  "Read a byte array of a given size starting at a given position.
   Operates on an already open RandomAccessFile object."
  [^RandomAccessFile raf position size]
  (println "getting" "offset" position "size" size)
  (let [bs (byte-array size)]
    (.seek raf position)
    (.read raf bs)
    bs))

(defn random-access-file
  "Simple clojure wrapper for creating a readonly RandomAccessFile object from
   a filename."
  [filename]
  (RandomAccessFile. (File. filename) "r"))

(defn get-bytes-from-back
  "Lazy-load binary data chunks from the end back to the front of the file."
  [filename]
  (let [positions (calculate-data-chunk-info filename)
        raf (random-access-file filename)]
    (map (fn [[p s]] (get-byte-array raf p s)) (reverse positions))))

(defn concat-byte-array
  "Slightly speed-optimized byte-array manipulation to avoid the overhead of
   converting to and from clojure seq."
  [a b]
  (let [new-array (byte-array (+ (count a) (count b)))]
    (System/arraycopy a 0 new-array 0 (count a))
    (System/arraycopy b 0 new-array (count a) (count b))
    new-array))

(defn hash-sequence
  "Takes a sequence of binary data and returns a list of the resulting
   sequential hashes. This assumes the data is coming in from last to first and
   does NOT change the output order. The last hash is still in front."
  [[last-data & rest-data :as data]]
  (let [final-hash (sha256 last-data)]
    (reductions (fn [prev-hash curr-data]
                  (sha256 (concat-byte-array curr-data prev-hash)))
                final-hash rest-data)))

(defn save-hashes-to-file
  "Write a list of binary-arrays as hex strings to `filename.shashes`."
  [filename hashes]
  (with-open [wrtr (io/writer (str filename ".shashes"))]
    (doseq [h hashes]
      (.write wrtr (str (hexify h 64) "\n")))))
