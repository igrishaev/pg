(defproject com.github.igrishaev/pg-client "0.1.11-SNAPSHOT"

  :description
  "Postgres client in pure Clojure (no JDBC)"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[org.clojure/clojure]
   [com.github.igrishaev/pg-common]
   [com.github.igrishaev/pg-types]]

  :java-source-paths ["java"]

  :java-cmd "/Users/wzhivga/work/jdk-21.jdk/Contents/Home/bin/java"
  ;; :javac-options ["--enable-preview" "--release" "20"]

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
    [[com.github.igrishaev/pg-json]
     [com.github.igrishaev/pg-integration]
     [com.github.igrishaev/pg-ssl]
     [com.github.igrishaev/pg-honey]

     [org.postgresql/postgresql]
     [com.github.seancorfield/next.jdbc]

     [org.clojure/data.csv]]}})
