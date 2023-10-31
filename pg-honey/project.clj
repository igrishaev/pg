(defproject com.github.igrishaev/pg-honey "0.1.10-SNAPSHOT"

  :description
  "HoneySQL wrapper for PG"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-client]
   [com.github.seancorfield/honeysql]]

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
