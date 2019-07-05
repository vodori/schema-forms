(ns schema-forms.utils
  (:require [schema.core :as s]
            [clojure.set :as sets]
            [clojure.string :as strings]
            [schema.experimental.abstract-map :as sam])
  (:import (clojure.lang IMeta)))

(defn schema-record? [s]
  (or (strings/starts-with? (.getName ^Class (class s)) "schema.core.")
      (strings/starts-with? (.getName ^Class (class s)) "schema.experimental.")))

(defn structure-schema? [schema]
  (or (instance? schema.experimental.abstract_map.AbstractSchema schema)
      (instance? schema.experimental.abstract_map.SchemaExtension schema)
      (and (map? schema) (not (schema-record? schema)))))

(defn map-vals [f m]
  (letfn [(f* [agg k v] (assoc! agg k (f v)))]
    (persistent! (reduce-kv f* (transient (or (empty m) {})) m))))

(defn filter-vals [pred m]
  (letfn [(f [agg k v] (if (pred v) (assoc! agg k v) agg))]
    (persistent! (reduce-kv f (transient (or (empty m) {})) m))))

(defn deep-merge [& maps]
  (letfn [(inner-merge [& maps]
            (let [ms (remove nil? maps)]
              (if (every? map? ms)
                (apply merge-with inner-merge ms)
                (last ms))))]
    (apply inner-merge maps)))

(defn real-key [k]
  (cond
    (or
      (instance? schema.core.RequiredKey k)
      (instance? schema.core.OptionalKey k))
    (real-key (:k k))
    :otherwise
    k))

(defn required-keys [m]
  (->> (keys m)
       (filter s/required-key?)
       (filter #(not= (get m %) s/Bool))
       (map real-key)
       (distinct)
       (vec)))

(defn title [s]
  (letfn [(norm [s]
            (or
              (some->> s
                (re-seq #"\w[a-z$]+")
                (map strings/lower-case)
                (map strings/capitalize)
                (strings/join " "))
              s))]
    (-> (cond
          (instance? IMeta s)
          (some-> s meta :name name)
          (string? s) s
          (keyword? s) (name s)
          :otherwise nil)
        norm)))

(defn enum-key? [k]
  (instance? schema.core.EnumSchema k))

(defn find-extra-keys-schema? [m]
  (let [extras #{s/Str s/Keyword (s/maybe s/Str) (s/maybe s/Keyword)}]
    (when-some [intersection (sets/intersection extras (set (keys m)))]
      (first intersection))))
