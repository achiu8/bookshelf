(defproject bookshelf "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace ["-Xmx1g" "-server"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [org.omcljs/om "0.9.0"]
                 [ring "1.4.0"]
                 [compojure "1.4.0"]
                 [com.datomic/datomic-free "0.9.5350" :exclusions [joda-time]]
                 [cljsjs/react "0.14.3-0"]
                 [cljsjs/react-dom "0.14.3-1"]
                 [sablono "0.6.2"]
                 [secretary "1.2.3"]]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0"]]


  :source-paths ["src/clj" "src/cljs"]
  :resource-paths ["resources"]
  :clean-targets ^{:protect false} ["resources/public/js/out"
                                    "resources/public/js/main.js"]

  :figwheel {:ring-handler bookshelf.core/handler}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/clj" "src/cljs"]
                        :figwheel true
                        :compiler {:output-to "resources/public/js/main.js"
                                   :output-dir "resources/public/js/out"
                                   :main bookshelf.core
                                   :asset-path "js/out"
                                   :optimizations :none
                                   :source-map true}}]})
