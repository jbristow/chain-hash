(ns chain-hash.core
  (:require [chain-hash.client :as client]
            [chain-hash.file :as f]
            [chain-hash.server :as server]
            [chain-hash.util :as util]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def chunk-size 1024)

(def options-spec
  [["-f" "--file FILENAME" "the file to operate on"]
   ["-p" "--piece N" "which piece to grab" :parse-fn #(Long/valueOf %)]
   ["-c" "--checksum HASH" "The checksum for the next piece."]
   ["-o" "--output FILENAME" "file to output fetched data"]])

(defn exit
  "Exists as a nice wrapper around system/exit."
  ([exit-code]
   (System/exit exit-code))
  ([] (exit 0)))

(defn -main
  "The main entry point for the cli"
  [& args]
  (let [{:keys [arguments options] :as po} (parse-opts args options-spec)
        arg (first arguments)]

    (try
      (cond
        (= "gen-hashes" arg)
        (server/generate-hashes-for (:file options) chunk-size)

        (= "count-pieces" arg)
        (println (client/count-pieces (:file options) chunk-size))

        (and (= "fetch-piece" arg)
             (contains? options :checksum)
             (contains? options :piece))
        (println (client/fetch-piece (:file options) (:piece options) chunk-size))

        (and  (= "fetch" arg)
              (contains? options :checksum))
        (let [{:keys [output checksum file]} options
              handle-output (if (nil? output) :default (partial f/append-to output))]
          (client/fetch file checksum handle-output chunk-size))

        :else
        (do (println "Problem validating command line arguments.")
            (println "Possible options: count-pieces, gen-hashes, fetch-piece, fetch")
            (println (:summary options))
            (exit 1)))
      (catch Exception e
        (do (println "Exception:" (.getMessage e))
            (when (ex-data e)
              (println (ex-data e))))))

    (exit 0)))
