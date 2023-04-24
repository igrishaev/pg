(defproject com.github.igrishaev/pg-oid "0.1.0-SNAPSHOT"

  :description
  "PG OID registry"

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
