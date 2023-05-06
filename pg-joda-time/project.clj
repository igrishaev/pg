(defproject com.github.igrishaev/pg-joda-time "0.1.1-SNAPSHOT"

  :description
  "Extend encoding & decoding with Joda Time"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-encode]
   [joda-time/joda-time]]

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
