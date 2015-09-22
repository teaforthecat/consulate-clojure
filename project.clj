(defproject consulate-simple "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [selmer "0.8.2"]
                 [com.taoensso/timbre "3.4.0"]
                 [com.taoensso/tower "3.0.2"]
                 [markdown-clj "0.9.66"]
                 [environ "1.0.0"]
                 [compojure "1.3.4"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-session-timeout "0.1.0"]
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.2"]
                 [bouncer "0.3.3"]
                 [prone "0.8.2"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [ring-server "0.4.0"]
                 [org.clojure/tools.reader "0.9.2"]
                 [reagent "0.5.0"]
                 [re-frame "0.4.1"]
                 [cljsjs/react "0.13.3-0"]
                 [reagent-forms "0.5.1"]
                 [reagent-utils "0.1.4"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [cljs-ajax "0.3.13"]
                 ;;new
                 [cheshire "5.4.0"]
                 [http-kit "2.1.16"]
                 [clojurewerkz/route-one "1.1.0"]
                 [byte-streams "0.2.0"]
                 [com.rpl/specter "0.6.1"]
                 [datascript "0.11.4"]
                 [cljs-http "0.1.35"]
                 ]

  :min-lein-version "2.0.0"
  :uberjar-name "consulate-simple.jar"
  :jvm-opts ["-server"]

  ;;enable to start the nREPL server when the application launches
                                        ;:env {:repl-port 7001}

  :main consulate-simple.core

  :plugins [[lein-ring "0.9.1"]
            [lein-environ "1.0.0"]
            [lein-ancient "0.6.5"]
            [lein-cljsbuild "1.0.6"]
            [lein-doo "0.1.5-SNAPSHOT"]
            [lein-sassy "1.0.7"]]

  :sass {:src "resources/app/stylesheets"
         :dst "resources/public/stylesheets"}

  :ring {:handler consulate-simple.handler/app
         :init    consulate-simple.handler/init
         :destroy consulate-simple.handler/destroy
         :uberwar-name "consulate-simple.war"}

  :clean-targets ^{:protect false} ["resources/public/js"]

  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src-cljs"]
     :compiler
     {:output-dir "resources/public/js/out"
      :externs ["react/externs/react.js"]
      :optimizations :none
      :output-to "resources/public/js/app.js"
      :warnings {:single-segment-namespace false}
      :pretty-print true}}
    :test {:source-paths ["src-cljs" "test/cljs"]
           :compiler
           {:output-to "resources/public/js/testable.js"
            :main "consulate-simple.runner"
            :optimizations :none}}}}


  :profiles
  {:uberjar {:omit-source true
             :env {:production true}
             :hooks [leiningen.cljsbuild
                     leiningen.sass]
             :cljsbuild
             {:jar true
              :builds
              {:app
               {:source-paths ["env/prod/cljs"]
                :compiler {:optimizations :advanced :pretty-print false}}}}

             :aot :all}
   :project/dev {:dependencies [[ring-mock "0.1.5"]
                                [ring/ring-devel "1.3.2"]
                                [pjstadig/humane-test-output "0.7.0"]
                                [lein-doo "0.1.5-SNAPSHOT"]
                                [lein-figwheel "0.3.3"]
                                [org.clojure/tools.nrepl "0.2.10"]]
                 :plugins [[lein-figwheel "0.3.3"]]

                 :cljsbuild
                 {:builds {:app {
                                  :source-paths ["env/dev/cljs"]
                                  :compiler {:source-map true
                                             :warnings {:single-segment-namespace false}
                                             }
                                  }}
                          }

                 :figwheel
                 {:http-server-root "public"
                  :server-port 3449
                  :nrepl-port 7002
                  :css-dirs ["resources/public/stylesheets"]
                  :ring-handler consulate-simple.handler/app}

                 :repl-options {:init-ns consulate-simple.core}
                 :injections [(require 'pjstadig.humane-test-output)
                              (pjstadig.humane-test-output/activate!)]
                 :env {:dev true}}
                                        ; merges the above with the :profiles/dev section in the untracked file: ./profiles.clj for env settings
   ;; consider :hooks [leiningen.sass]

   :dev [:project/dev :profiles/dev]})
