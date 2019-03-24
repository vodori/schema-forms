(ns schema-forms.core
  (:require [schema-forms.schemafy :as schemafy]
            [schema-forms.bijections :as bijections]))


(defn prismatic->json-schema
  "Convert a prismatic schema into a json schema compatible with
   react-jsonschema-form. Optionally supply your own bijections."
  ([schema] (prismatic->json-schema schema {}))
  ([schema {:keys [bijections ctx]
            :or   {bijections bijections/DEFAULT_BIJECTIONS
                   ctx        {}}}]
   (let [bijected (bijections/prismatic-schema->json-schema schema bijections)]
     (schemafy/schemafy bijected ctx))))


(defn prismatic-data->json-schema-data
  "Convert data that conforms to a prismatic schema into data that would be
   compatible with the json-schema produced from that prismatic schema, including
   any supplied bijections."
  ([schema data]
   (prismatic-data->json-schema-data schema data bijections/DEFAULT_BIJECTIONS))
  ([schema data bijections]
   (bijections/prismatic-data->json-schema-data schema data bijections)))


(defn json-schema-data->prismatic-data
  "Convert data that conforms to a json schema into data that would be
   compatible with the prismatic schema provided, including reversal of
   any supplied bijections."
  ([schema data]
   (json-schema-data->prismatic-data schema data bijections/DEFAULT_BIJECTIONS))
  ([schema data bijections]
   (bijections/json-schema-data->prismatic-data schema data bijections)))


