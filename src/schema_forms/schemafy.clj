(ns schema-forms.schemafy
  (:require [schema-forms.utils :as utils]
            [schema.core :as s])
  (:import (java.util Map Set List)
           (clojure.lang MapEntry Symbol Keyword)
           (java.util.regex Pattern)))

(defmulti bottom-out
  (fn [x] (if (class? x) x (class x))))

(defmethod bottom-out String [_] {:type "string"})
(defmethod bottom-out Number [_] {:type "number"})
(defmethod bottom-out Integer [_] {:type "integer"})
(defmethod bottom-out Boolean [_] {:type "boolean"})
(defmethod bottom-out Map [_] {:type "object"})
(defmethod bottom-out List [_] {:type "array"})
(defmethod bottom-out Set [_] {:type "array"})
(defmethod bottom-out Keyword [_] {:type "string"})
(defmethod bottom-out Pattern [_] {:type "string"})
(defmethod bottom-out Symbol [_] {:type "string"})
(defmethod bottom-out (class boolean?) [_] {:type "boolean"})
(defmethod bottom-out (class number?) [_] {:type "number"})
(defmethod bottom-out (class integer?) [_] {:type "integer"})
(defmethod bottom-out (class string?) [_] {:type "string"})
(defmethod bottom-out (class keyword?) [_] {:type "string"})
(defmethod bottom-out (class symbol?) [_] {:type "string"})

(prefer-method bottom-out Integer Number)

(prefer-method bottom-out (class integer?) (class number?))


(defprotocol JsonSchemafy
  (schemafy [x opts]))


(extend-protocol JsonSchemafy

  Class
  (schemafy [x opts]
    (bottom-out x))

  MapEntry
  (schemafy [[k v] opts]
    (merge (schemafy v opts) {:title (utils/title (utils/real-key k))}))

  schema.core.AnythingSchema
  (schemafy [x opts]
    (throw (ex-info "Unsupported s/Any schema!" {:property (get opts :path)})))

  schema.core.One
  (schemafy [{:keys [schema]} opts]
    (schemafy schema opts))

  schema.core.EqSchema
  (schemafy [{:keys [v]} opts]
    (merge (bottom-out v) {:enum #{v}}))

  schema.core.NamedSchema
  (schemafy [{:keys [schema] :as n} opts]
    (utils/filter-vals some?
      (assoc (schemafy schema opts) :title (some-> n :name name))))

  schema.core.Maybe
  (schemafy [{:keys [schema]} opts]
    (schemafy schema opts))

  schema.core.Predicate
  (schemafy [{:keys [p? pred-name]} opts]
    (utils/filter-vals some?
      (assoc (bottom-out p?) :title (some-> pred-name name))))

  schema.core.Recursive
  (schemafy [{:keys [derefable]} opts]
    (schemafy @derefable opts))

  schema.core.Either
  (schemafy [{:keys [schemas]} opts]
    {:anyOf (mapv #(schemafy % opts) schemas)})

  schema.core.Both
  (schemafy [{:keys [schemas]} opts]
    {:allOf (mapv #(schemafy % opts) schemas)})

  schema.core.Constrained
  (schemafy [{:keys [schema]} opts]
    (schemafy schema opts))

  schema.core.EnumSchema
  (schemafy [{:keys [vs]} opts]
    (if (empty? vs)
      (throw (ex-info "Empty enumeration!" {:property (get opts :path)}))
      (merge (bottom-out (first vs)) {:enum vs})))

  schema.experimental.abstract_map.SchemaExtension
  (schemafy [{:keys [base-schema extended-schema]} opts]
    (let [{:keys [sub-schemas dispatch-key schema]} base-schema
          reversed (->> (utils/map-vals :extended-schema @sub-schemas)
                        (group-by val)
                        (utils/map-vals (partial map key))
                        (utils/map-vals set))]
      (-> (utils/deep-merge schema extended-schema {dispatch-key (apply s/enum (reversed extended-schema))})
          (schemafy opts))))

  schema.experimental.abstract_map.AbstractSchema
  (schemafy [{:keys [sub-schemas schema dispatch-key]} opts]
    (let [subs @sub-schemas]
      {:type         "object"
       :properties   {dispatch-key {:type "string" :enum (disj (set (keys subs)) nil)}}
       :required     [dispatch-key]
       :dependencies {dispatch-key
                      {:oneOf
                       (vec (for [[k v] subs :when (some? k)]
                              (let [extension (get v :extended-schema)]
                                (utils/deep-merge
                                  (-> (merge schema (dissoc extension dispatch-key))
                                      (schemafy (update opts :path (fnil conj []) {dispatch-key k})))
                                  {:properties {dispatch-key {:enum [k]}}}))))}}}))

  List
  (schemafy [s opts]
    (cond
      (empty? s)
      (throw (ex-info "Empty vector!" {:property (get opts :path)}))

      (and (= 1 (count s)) (not (instance? schema.core.One (first s))))
      (utils/filter-vals some?
        {:type  "array"
         :title (utils/title s)
         :items (schemafy (first s) (update opts :path (fnil conj []) 0))})

      :otherwise
      (let [positional? (partial instance? schema.core.One)
            optional?   (every-pred positional? (comp boolean :optional?))
            groups      (group-by (juxt positional? optional?) s)
            required    (get groups [true false] [])
            optional    (get groups [true true] [])
            repeated    (get groups [false false] [])
            items       (->> (concat required optional repeated)
                             (map-indexed #(schemafy %2 (update opts :path (fnil conj []) %1))))]

        (utils/filter-vals some?
          {:type     "array"
           :title    (utils/title s)
           :items    (if (= 1 (count items)) (first items) items)
           :maxItems (when (empty? repeated)
                       (+ (count required) (count optional)))
           :minItems (count required)}))))

  Set
  (schemafy [s opts]
    (if (empty? s)
      (throw (ex-info "Empty set!" {:property (get opts :path)}))
      (utils/filter-vals some?
        {:type        "array"
         :title       (utils/title s)
         :items       (schemafy (first s) (update opts :path (fnil conj []) 0))
         :uniqueItems true})))

  Map
  (schemafy [schema opts]
    (let [props
          (->> (for [[k v :as e] schema]
                 (cond
                   (s/specific-key? k)
                   [[(utils/real-key k) (schemafy e (update opts :path (fnil conj []) k))]]

                   (utils/enum-key? k)
                   (for [kk (:vs k [])]
                     (let [entry (first {(s/optional-key (keyword kk)) v})]
                       [(utils/real-key kk) (schemafy entry (update opts :path (fnil conj []) kk))]))

                   :otherwise
                   []))
               (mapcat identity)
               (into {}))
          additional
          (if-some [extras (utils/find-extra-keys-schema? schema)]
            (schemafy (get schema extras) opts)
            false)
          required
          (utils/required-keys schema)]
      (utils/filter-vals some?
        {:type                 "object"
         :title                (utils/title schema)
         :properties           props
         :additionalProperties additional
         :required             (when-not (empty? required) required)}))))
