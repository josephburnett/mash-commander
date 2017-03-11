(defproject mash-commander "0.1.0-SNAPSHOT"
  :description "Fun typing for kids!"
  :url "https://github.com/josephburnett/mash-commander"
  :license {:name "MIT"
            :url "https://github.com/josephburnett/mash-commander/blob/master/LICENSE"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.395"
                  :exclusions [org.clojure/tools.reader]]
                 [cljsjs/react "15.4.2-1"]
                 [cljsjs/react-dom "15.4.2-1"]
                 [sablono "0.7.7"]
                 [org.omcljs/om "1.0.0-alpha46"
                  :exclusions
                  [com.fasterxml.jackson.core/jackson-core]]
                 [cljs-ajax "0.5.8"
                  :exclusions
                  [com.fasterxml.jackson.core/jackson-core
                   com.fasterxml.jackson.dataformat/jackson-dataformat-cbor
                   com.fasterxml.jackson.dataformat/jackson-dataformat-smile]]
                 [org.clojure/data.json "0.2.6"]
                 [amazonica "0.3.89"]
                 [digest "1.4.5"]
                 [cljs-hash "0.0.2"]]

  :plugins [[lein-figwheel "0.5.9"]
            [lein-cljsbuild "1.1.5" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel {:open-urls ["http://localhost:3449/index.html"]
                           :websocket-host :js-client-host}
                :compiler {:main mash-commander.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/mash_commander.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/mash_commander.js"
                           :main mash-commander.core
                           :optimizations :simple ; advanced optimizations break AWS Polly SDK
                           :externs ["externs/aws-sdk-polly-2.16.0.min.js"]
                           :pretty-print false}}]}

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.0"]
                                  [figwheel-sidecar "0.5.9"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   :source-paths ["src" "dev"]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :aliases {"generate" ["run" "-m" "mash-commander.generate" "main" :project/root]}
)
