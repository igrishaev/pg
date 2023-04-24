(defproject com.github.igrishaev/pg-common "0.1.0-SNAPSHOT"

  :description
  "Common PG modules"

  :plugins
  [[lein-parent "0.3.8"]]

  :parent-project
  {:path "../project.clj"
   :inherit [:deploy-repositories
             :license
             :managed-dependencies
             :plugins
             :repositories
             :url
             [:profiles :dev]]})