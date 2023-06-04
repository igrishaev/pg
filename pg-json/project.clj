(defproject com.github.igrishaev/pg-json "0.1.1-SNAPSHOT"

  :description
  "Extend JSON(b) encoding & decoding with Cheshire"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-encode]
   [com.github.igrishaev/pg-decode]
   [cheshire]]

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
