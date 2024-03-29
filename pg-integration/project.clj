(defproject com.github.igrishaev/pg-integration "0.1.12-SNAPSHOT"

  :description
  "Multi-version integration tests in Docker"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-common]
   [com.github.igrishaev/pg-ssl]]

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
