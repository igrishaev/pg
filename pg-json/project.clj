(defproject com.github.igrishaev/pg-json "0.1.1"

  :description
  "Expand JSON(b) encoding & decoding with Cheshire"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-types]
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
