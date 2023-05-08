(defproject com.github.igrishaev/pg-client "0.1.1-SNAPSHOT"

  :description
  "PG client"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-common]
   [com.github.igrishaev/pg-encode]]

  :parent-project
  {:path "../project.clj"
   :inherit [:deploy-repositories
             :license
             :release-tasks
             :managed-dependencies
             :plugins
             :repositories
             :url
             [:profiles :dev]]})
