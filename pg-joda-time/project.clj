(defproject com.github.igrishaev/pg-joda-time "0.1.11-SNAPSHOT"

  :description
  "Expand encoding & decoding with Joda Time"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-types]
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
             [:profiles :dev]]}

  :profiles
  {:test
   {:dependencies
    [[clj-time]]}})
