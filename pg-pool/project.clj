(defproject com.github.igrishaev/pg-pool "0.1.10"

  :description
  "A simple, handmade connection pool for PG"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-client]
   [org.clojure/tools.logging]]

  :parent-project
  {:path "../project.clj"
   :inherit [:deploy-repositories
             :license
             :release-tasks
             :managed-dependencies
             :plugins
             :repositories
             :url
             [:profiles :dev]]}

  :profiles
  {:test
   {:dependencies
    [[com.github.igrishaev/pg-json]
     [com.github.igrishaev/pg-integration]
     [ch.qos.logback/logback-classic]
     [com.stuartsierra/component]]}})
