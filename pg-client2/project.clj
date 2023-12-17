(defproject com.github.igrishaev/pg-client2 "0.1.11-SNAPSHOT"

  :description
  "Postgres client in pure Clojure^W Java (no JDBC)"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[org.clojure/clojure]
   [metosin/jsonista]]

  :java-source-paths ["src"]

  ;; :java-cmd "/Users/wzhivga/work/jdk-21.jdk/Contents/Home/bin/java"
  :java-cmd "/Users/ivan/work/jdk-21.jdk/Contents/Home/bin/java"
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
    [[com.github.seancorfield/next.jdbc]
     [org.postgresql/postgresql]
     [org.clojure/data.csv]
     [criterium]]}})
