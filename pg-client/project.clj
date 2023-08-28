(defproject com.github.igrishaev/pg-client "0.1.7-SNAPSHOT"

  :description
  "Postgres client in pure Clojure (no JDBC)"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-common]
   [com.github.igrishaev/pg-types]]

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
     [com.github.igrishaev/pg-integration]]}})
