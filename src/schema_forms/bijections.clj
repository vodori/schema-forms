(ns schema-forms.bijections
  (:require [com.gfredericks.schema-bijections :as sbi]
            [schema.experimental.abstract-map :as sam]
            [schema-forms.walkers :as walkers]              ; for side effects
            [schema-forms.utils :as utils]
            [schema.core :as s]))

(defn optional-map->array-of-0-or-1 [s]
  (when (and (instance? schema.core.Maybe s) (utils/structure-schema? (:schema s)))
    {:left        [(s/optional (:schema s) "")]
     :left->right first
     :right->left #(filterv some? [%])
     :right       s}))


(defn anything-schema->polymorphic-container [s]
  (when (instance? schema.core.AnythingSchema s)
    (letfn [(kind-for-value [v]
              (let [multi? (vector? v)
                    val    (if multi? (first v) v)
                    type   (cond
                             (string? val) "STRING"
                             (boolean? val) "BOOLEAN"
                             (number? val) "NUMBER")]
                (keyword (str type (when multi? "_ARRAY")))))]
      {:left        (let [field (sam/abstract-map-schema :kind {})]
                      (sam/extend-schema! field {:value s/Str} 'StringField [:STRING])
                      (sam/extend-schema! field {:value [s/Str]} 'StringArrayField [:STRING_ARRAY])
                      (sam/extend-schema! field {:value s/Num} 'NumberField [:NUMBER])
                      (sam/extend-schema! field {:value [s/Num]} 'NumberArrayField [:NUMBER_ARRAY])
                      (sam/extend-schema! field {:value s/Bool} 'BooleanField [:BOOLEAN])
                      field)
       :left->right (fn [x] (get x :value))
       :right->left (fn [x] {:value x :kind (kind-for-value x)})
       :right       s})))


(def DEFAULT_BIJECTIONS
  [optional-map->array-of-0-or-1
   anything-schema->polymorphic-container])

(defn prismatic-schema->json-schema
  "To convert a schema into the interaction optimized version."
  ([schema]
   (prismatic-schema->json-schema schema DEFAULT_BIJECTIONS))
  ([schema bijections]
   (:left (sbi/schema->bijection schema bijections))))

(defn prismatic-data->json-schema-data
  "To convert data that conforms to an original schema into data that conforms
   to the interaction optimized schema. Schema is the original schema."
  ([schema data]
   (prismatic-data->json-schema-data schema data DEFAULT_BIJECTIONS))
  ([schema data bijections]
   ((:right->left (sbi/schema->bijection schema bijections)) data)))

(defn json-schema-data->prismatic-data
  "To convert data that conforms to the interaction optimized schema
   into data that conforms to the original schema. Schema is the original schema."
  ([schema data]
   (json-schema-data->prismatic-data schema data DEFAULT_BIJECTIONS))
  ([schema data bijections]
   ((:left->right (sbi/schema->bijection schema bijections)) data)))
