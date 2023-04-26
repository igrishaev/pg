(defproject com.github.igrishaev/pg-copy "0.1.0-SNAPSHOT"

  :description
  "COPY within PG binary format"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-encode]]

  :parent-project
  {:path "../project.clj"
   :inherit [:deploy-repositories
             :license
             :managed-dependencies
             :plugins
             :repositories
             :url
             [:profiles :dev]]}

  :profiles
  {:test
   {:dependencies
    [[com.github.seancorfield/next.jdbc]
     [org.postgresql/postgresql]
     [com.github.igrishaev/pg-joda-time]]}})
