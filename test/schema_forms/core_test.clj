(ns schema-forms.core-test
  (:require [clojure.test :refer :all]
            [schema-forms.core :refer :all]
            [schema.core :as s]
            [schema-forms.helpers :refer :all]))


(s/defschema RecursivePerson
  {:firstName s/Str
   :friends   [(s/recursive #'RecursivePerson)]})

(deftest prismatic->json-schema-test
  (testing "Simple schema"
    (let [schema {:firstName s/Str :lastName s/Str :age s/Num}]
      (check-expected (s/named schema "SimplePerson"))))

  (testing "Optional fields"
    (let [schema {:firstName                 s/Str
                  (s/optional-key :lastName) (s/maybe s/Str)}]
      (check-expected (s/named schema "OptionalSurname"))))

  (testing "Repeat unbounded list"
    (let [schema {:favoriteDogs [{:name s/Str :breed (s/enum "Collie" "Husky")}]}]
      (check-expected (s/named schema "RepeatList"))))

  (testing "Optional position item list"
    (let [schema {:favoriteDogs [(s/optional {:name s/Str :breed (s/enum "Collie" "Husky")} "Dog")]}]
      (check-expected (s/named schema "Single Optional Position List"))))

  (testing "Required position item list"
    (let [schema {:favoriteDogs [(s/one {:name s/Str :breed (s/enum "Collie" "Husky")} "Dog")]}]
      (check-expected (s/named schema "Single Required Position List"))))

  (testing "Recursive schemas"
    (check-expected RecursivePerson)))


(deftest prismatic-data->json-schema-data-test
  )

(deftest json-schema-data->prismatic-data-test
  )
