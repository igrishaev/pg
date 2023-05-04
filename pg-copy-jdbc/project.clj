(defproject com.github.igrishaev/pg-copy-jdbc "0.1.0-SNAPSHOT"

  :description
  "COPY powered with JDBC.next"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-copy]
   [org.postgresql/postgresql]]

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
    [[com.github.igrishaev/pg-joda-time]
     [com.github.seancorfield/next.jdbc]]}})
