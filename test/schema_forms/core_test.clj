(ns schema-forms.core-test
  (:require [clojure.test :refer :all]
            [schema-forms.core :refer :all]
            [schema.core :as s]))



(deftest prismatic->json-schema-test
  (testing "Simple schema"
    (let [person {:firstName s/Str :lastName s/Str :age s/Num}]
      (is (= {:type
              "object",
              :properties
              {:firstName {:type "string", :title "First Name"},
               :lastName  {:type "string", :title "Last Name"},
               :age       {:type "number", :title "Age"}},
              :additionalProperties
              false,
              :required
              [:firstName :lastName :age]}
             (prismatic->json-schema person))))))


(deftest prismatic-data->json-schema-data-test
  )


(deftest json-schema-data->prismatic-data-test
  )
