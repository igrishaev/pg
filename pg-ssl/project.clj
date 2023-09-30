(defproject com.github.igrishaev/pg-ssl "0.1.9-SNAPSHOT"

  :description
  "SSL-related utils"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[less-awful-ssl]]

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
    []}})
