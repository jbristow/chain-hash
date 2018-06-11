(defproject chain-hash "0.1.0-SNAPSHOT"
  :description "craft-demo showing off sequential hashing"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.cli "0.3.7"]]
  :main ^:skip-aot chain-hash.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :javac-options ["-target" "1.10"
                  "-source" "1.10"
                  "-Xlint:-options"])

