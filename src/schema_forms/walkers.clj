(ns schema-forms.walkers
  "Additional walk implementations for schema-bijections to handle more composite schemas."
  (:require [schema.core :as s]
            [com.gfredericks.schema-bijections :as sbi]
            [schema.experimental.abstract-map :as sam]
            [schema-forms.utils :as utils]))


; TODO: submit these implementations upstream so that bijections handles
; the bulk of schema by default.

(defmethod sbi/walk* schema.experimental.abstract_map.AbstractSchema
  [{:keys [sub-schemas dispatch-key schema] :as abstract} bijector]
  (let [extensions @sub-schemas]
    (letfn [(delegate-to-map-impl [transform]
              (fn [v]
                (let [dispatch-val    (let [dv (get v dispatch-key)]
                                        (cond
                                          (and (nil? dv) (contains? extensions dv))
                                          nil
                                          (contains? extensions (keyword dv))
                                          (keyword dv)
                                          (contains? extensions (name dv))
                                          (name dv)
                                          :otherwise
                                          (throw (ex-info "Bijection schema error, invalid dispatch value." {}))))
                      extended-schema (:extended-schema (get extensions dispatch-val))
                      map-schema      (merge schema extended-schema {dispatch-key (s/enum dispatch-val)})]
                  ((transform (sbi/walk map-schema bijector)) (assoc v dispatch-key dispatch-val)))))]
      {:left        (let [abstract (sam/abstract-map-schema dispatch-key (:left (sbi/walk schema bijector)))]
                      (doseq [[k {:keys [extended-schema schema-name]}] extensions]
                        (sam/extend-schema! abstract (:left (sbi/walk extended-schema bijector)) schema-name [k]))
                      abstract)
       :left->right (delegate-to-map-impl :left->right)
       :right->left (delegate-to-map-impl :right->left)
       :right       abstract})))

(defmethod sbi/walk* schema.experimental.abstract_map.SchemaExtension
  [{:keys [schema-name base-schema extended-schema] :as extension} bijector]
  (let [{:keys [sub-schemas dispatch-key schema]} base-schema
        schema->dispatch-keys (->> (utils/map-vals :extended-schema @sub-schemas)
                                   (group-by val)
                                   (utils/map-vals (partial map key))
                                   (utils/map-vals set))
        dispatch-keys         (vec (schema->dispatch-keys extended-schema))]
    (letfn [(delegate-to-map-impl [transform]
              (fn [v]
                (let [map-schema (merge schema extended-schema {dispatch-key (apply s/enum dispatch-keys)})]
                  ((transform (sbi/walk map-schema bijector)) v))))]
      {:left        (let [abstract (sam/abstract-map-schema dispatch-key (:left (sbi/walk schema bijector)))]
                      (sam/extend-schema! abstract (:left (sbi/walk extended-schema bijector)) schema-name dispatch-keys))
       :left->right (delegate-to-map-impl :left->right)
       :right->left (delegate-to-map-impl :right->left)
       :right       extension})))


(defmethod sbi/walk* schema.core.Constrained
  [{:keys [schema postcondition post-name] :as s} bijector]
  (let [{:keys [left left->right right->left]} (sbi/walk schema bijector)]
    {:left        (s/constrained left postcondition post-name)
     :left->right left->right
     :right->left right->left
     :right       s}))
