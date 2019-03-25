(ns schema-forms.helpers
  (:require [clojure.test :refer :all]
            [schema-forms.core :refer :all]
            [clojure.java.browse :as browse]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cheshire.core :as chesh]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint])
  (:import (java.io File)))

(def ^:dynamic *testing* true)

(defmacro check-expected [& schema]
  `(let [schema# (do ~@schema)
         actual# (prismatic->json-schema schema#)
         path#   (format "schemas/%s.edn"
                         (if (instance? schema.core.NamedSchema schema#)
                           (some-> schema# :name)
                           (some-> schema# meta :name)))]
     (if *testing*
       (is (= actual# (edn/read-string (slurp (io/resource path#)))))
       (spit (io/file (str "testfiles/" path#)) (with-out-str (pprint/pprint actual#))))))

(defn regenerate-expectations []
  (binding [*testing* false] (run-tests)))

(defn regenerate-samples []
  (regenerate-expectations)
  (let [placeholder "{{JSON_SCHEMA_PLACEHOLDER}}"
        template    (slurp (io/resource "example/index.html"))]
    (doseq [file (file-seq (io/file (io/resource "schemas")))
            :when (.isFile ^File file)]
      (let [data        (edn/read-string (slurp file))
            as-json     (chesh/generate-string data)
            replaced    (string/replace-first template placeholder as-json)
            output      (format "testfiles/samples/%s" (string/replace (.getName file) ".edn" ".html"))
            output-file (io/file output)]
        (spit output-file replaced)))))

(defn view-samples []
  (regenerate-samples)
  (doseq [file (file-seq (io/file (io/resource "samples")))
          :when (.isFile ^File file)]
    (browse/browse-url (.getAbsolutePath file))))
