(defproject com.vodori/schema-forms "0.1.0-SNAPSHOT"

  :description
  "A library for converting prismatic schemas into json-schemas for building form fields."

  :url
  "https://github.com/vodori/schema-forms"

  :license
  {:name "MIT License" :url "http://opensource.org/licenses/MIT" :year 2019 :key "mit"}

  :scm
  {:name "git" :url "https://github.com/vodori/schema-forms"}

  :pom-addition
  [:developers
   [:developer
    [:name "Paul Rutledge"]
    [:url "https://github.com/rutledgepaulv"]
    [:email "paul.rutledge@vodori.com"]
    [:timezone "-5"]]]

  :deploy-repositories
  {"releases"  {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/" :creds :gpg}
   "snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots/" :creds :gpg}}

  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [prismatic/schema "1.1.11"]
   [com.gfredericks/schema-bijections "0.1.3"]]

  :profiles
  {:test {:dependencies [[cheshire "5.8.1" :scope "test"]]
          :resource-paths ["testfiles"]}})
