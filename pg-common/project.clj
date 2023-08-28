(defproject com.github.igrishaev/pg-common "0.1.2"

  :description
  "Common PG modules"

  :plugins
  [[lein-parent "0.3.8"]]

  :profiles
  {:tasks
   {:source-paths ["tasks"]}}

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
