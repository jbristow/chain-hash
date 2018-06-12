(ns chain-hash.core
  (:require [chain-hash.client :as client]
            [chain-hash.file :as f]
            [chain-hash.server :as server]
            [clojure.tools.cli :refer [parse-opts]])
  (:import (java.lang Long))
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

(defn missing-required-argument
  "Simple wrapper for consistent argument errors."
  [which]
  (ex-info "Missing required argument" {:missing-argument (name which)}))

(defmacro validate-has-option
  "Validate that the listed options all exist. Elides a nested cond and a bunch
   of unnecessarily repeated code."
  [option olist body]
  (if (seq olist)
    (let [[curr-opt & r-opts] olist]
      (list 'if
            (list 'contains? option curr-opt)
            (if (seq r-opts)
              (cons 'chain-hash.core/validate-has-option (list option (apply vector r-opts) body))
              body)
            (list 'throw (list 'missing-required-argument curr-opt))))
    body))

(defn check-file-exists [filename]
  (when-not (f/exists? filename)
    ()))

(defn -main
  "The main entry point for the cli"
  [& args]
  (let [{:keys [arguments options errors] :as po} (parse-opts args options-spec)
        arg (first arguments)]
    (try
      (cond
        (seq errors)
        (throw (ex-info "Problems parsing command line options"  {:errors errors}))

        (= "gen-hashes" arg)
        (validate-has-option
         options [:file]
         (server/generate-hashes-for (:file options) chunk-size))

        (= "count-pieces" arg)
        (validate-has-option
         options [:file]
         (println (client/count-pieces (:file options) chunk-size)))

        (= "fetch-piece" arg)
        (validate-has-option
         options [:checksum :piece :file]
         (println (client/fetch-piece (:file options) (:piece options) chunk-size)))

        (= "fetch" arg)
        (validate-has-option
         options [:checksum :file]
         (let [{:keys [output checksum file]} options
               output-handler (if (nil? output) :default (client/file-append-handler output))]
           (if (and (not= output-handler :default)
                    (f/exists? output))
             (throw (ex-info "Output file already exists." {:filename output}))
             (client/fetch file checksum chunk-size output-handler))))

        (= "encode" arg)
        (validate-has-option
         options [:file :output]
         (if-not (f/exists? (:output options))
           (server/encode (:file options) (:output options) chunk-size)
           (throw (ex-info "Output file already exists." {:filename (:output options)}))))

        (= "decode" arg)
        (validate-has-option
         options [:file :output :checksum]
         (if-not (f/exists? (:output options))
           (client/decode (:file options) (:output options) (:checksum options) chunk-size)
           (throw (ex-info "Output file already exists." {:filename (:output options)}))))

        :else
        (do (println "Problem validating command line arguments.")
            (println "Possible options: encode, decode, count-pieces, gen-hashes, fetch-piece, fetch")
            (println (:summary options))
            (exit 1)))
      (catch Exception e
        (do (println "Exception:" (.getMessage e))
            (println (or (ex-data e) e)))))

    (exit 0)))
