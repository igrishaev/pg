(defproject com.github.igrishaev/pg-encode "0.1.0-SNAPSHOT"

  :description
  "PG data encoding"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg-oid]]

  :parent-project
  {:path "../project.clj"
   :inherit [:deploy-repositories
             :license
             :managed-dependencies
             :plugins
             :repositories
             :url
             [:profiles :dev]]})