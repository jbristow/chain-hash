(ns chain-hash.util
  (:require [clojure.string :as str])
  (:import (java.security MessageDigest)
           (java.util Base64)))

(defn hexify
  "Returns the hex-string of a ByteArray. If a minimum length is added, the
   resulting hex will be left-padded with zeros.

   Based on https://stackoverflow.com/questions/10062967/clojures-equivalent-to-pythons-encodehex-and-decodehex#10065003"
  ([bs]
   (hexify bs 0))
  ([bs min-len]
   (let [raw (map #(format (str "%02x") (bit-and 0xff %)) bs)]
     (str/join "" (concat (repeat (max 0 (- min-len (* 2 (count raw)))) "0") raw)))))

(defn unhexify
  "returns the byte-array of a hex-string"
  [s]
  (byte-array (map (comp #(Long/parseLong % 16)
                         (partial str/join ""))
                   (partition 2 s))))

(defn sha256
  "Returns the hash of a ByteArray as a new ByteArray.

   Based on https://gist.github.com/kubek2k/8446062"
  [bs]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (.digest md bs)))

(defn base64-encode
  "Simple wrapper to easily convert byte-arrays into base64 encoded strings"
  [bs]
  (let [encoder (Base64/getEncoder)]
    (.encodeToString encoder bs)))
