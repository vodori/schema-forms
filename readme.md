[![Build Status](https://travis-ci.org/vodori/schema-forms.svg?branch=master)](https://travis-ci.org/vodori/schema-forms) [![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/vodori/schema-forms/maven-metadata.xml.svg)](https://mvnrepository.com/artifact/com.vodori/schema-forms)

### schema-forms

A Clojure library providing extensive conversions from prismatic schemas into json-schemas that are 
compatible for use with [react-jsonschema-form](https://github.com/mozilla-services/react-jsonschema-form).

### origin

We use this functionality to generate an admin UI from schemas that define configurable data 
in our applications. The conversion from prismatic schema to json-schema is likely the most
useful piece of this work but in the future we may open source the admin interface as well.

___

### installation 

```clojure
[com.vodori/schema-forms "0.1.0"]
```

___

### usage 

```clojure
(require '[schema.core :as s])
(require '[schema-forms.core :as sf])


; simple example

(s/defschema Person 
  {:firstName s/Str
   :lastName  s/Str 
   :age       s/Num})

(def json-schema (sf/prismatic->json-schema Person))

#_{:type "object",
   :title "Person",
   :properties
   {:firstName {:type "string", :title "First Name"},
    :lastName {:type "string", :title "Last Name"},
    :age {:type "number", :title "Age"}},
   :additionalProperties false,
   :required [:firstName :lastName :age]}

; https://jsfiddle.net/e7prdzkq/


; recursive schemas

(s/defschema RecursivePerson
  {:firstName s/Str
   :friends   [(s/recursive #'RecursivePerson)]})
   
(def json-schema (sf/prismatic->json-schema RecursivePerson))

#_{:type "object",
   :title "Recursive Person",
   :properties
   {:firstName {:type "string", :title "First Name"},
    :friends
    {:type "array",
     :items
     {"$ref" "#/definitions/schema-forms.core-test.RecursivePerson"},
     :minItems 0,
     :title "Friends"}},
   :additionalProperties false,
   :required [:firstName :friends],
   :definitions
   {"schema-forms.core-test.RecursivePerson"
    {:type "object",
     :title "Recursive Person",
     :properties
     {:firstName {:type "string", :title "First Name"},
      :friends
      {:type "array",
       :items
       {"$ref" "#/definitions/schema-forms.core-test.RecursivePerson"},
       :minItems 0,
       :title "Friends"}},
     :additionalProperties false,
     :required [:firstName :friends]}}}

; https://jsfiddle.net/zy4mogx5/


; abstract maps

(require '[schema.experimental.abstract-map :as sam])

(s/defschema Location
  (sam/abstract-map-schema :country
    {:street s/Str}))

(sam/extend-schema UnitedStatesLocation
  Location ["UNITED_STATES"]
  {:state (s/enum "ALABAMA" "ALASKA")})

(sam/extend-schema CanadianLocation
  Location ["CANADA"]
  {:province (s/enum "ALBERTA" "BRITISH_COLUMBIA")})
  
(def json-schema (sf/prismatic->json-schema Location))

#_{:type "object",
   :properties
   {:country {:type "string", :enum #{"CANADA" "UNITED_STATES"}}},
   :required [:country],
   :dependencies
   {:country
    {:oneOf
     [{:type "object",
       :properties
       {:street {:type "string", :title "Street"},
        :state
        {:type "string", :enum #{"ALASKA" "ALABAMA"}, :title "State"},
        :country {:enum ["UNITED_STATES"]}},
       :additionalProperties false,
       :required [:street :state]}
      {:type "object",
       :properties
       {:street {:type "string", :title "Street"},
        :province
        {:type "string",
         :enum #{"ALBERTA" "BRITISH_COLUMBIA"},
         :title "Province"},
        :country {:enum ["CANADA"]}},
       :additionalProperties false,
       :required [:street :province]}]}}}

; https://jsfiddle.net/e7prdzkq/1/
       
```

___


### bijections

It's common when you start trying to programmatically generate forms that sometimes the schema
you have isn't exactly the same as how you want it to display within the form. We bridge the 
concept of similar but not the same by using [schema bijections](https://github.com/gfredericks/schema-bijections). 
Schema bijections provide a way to define transformations for schemas and data from the shape that is most suitable 
for your codebase and the shape that is most suitable for use with react-jsonschema-form.


A bijection is a function that receives a schema and returns a map describing the transformation process
or else returns nil if the schema it received shouldn't be transformed. Here's an example bijection that
converts any prismatic schema or data of the form `(s/maybe {:firstName s/Str})` into 
`[(s/optional {:firstName s/Str})]` instead. The array version produces a more desirable user experience
in the form when using react-jsonschema-form.


```clojure
(defn optional-maps-to-arrays-of-0-or-1-items [schema]
  (when (and (instance? schema.core.Maybe schema) (map? (:schema schema)))
    {:left        [(s/optional (:schema schema) "")]
     :left->right (fn [values] (first values))
     :right->left (fn [value] (filterv some? [value]))
     :right       schema}))

(s/defschema Person
  {:firstName              s/Str
   :lastName               s/Str
   (s/optional-key :friend) (s/maybe {:firstName s/Str
                                      :lastName  s/Str 
                                      :age       s/Num})})
     
(def options {:bijections [optional-maps-to-arrays-of-0-or-1-items]})
(def json-schema (sf/prismatic->json-schema Person options))

#_{:type "object",
   :title "Person",
   :properties
   {:firstName {:type "string", :title "First Name"},
    :lastName {:type "string", :title "Last Name"},
    :friend
    {:type "array",
     :items
     {:type "object",
      :title "Person",
      :properties
      {:firstName {:type "string", :title "First Name"},
       :lastName {:type "string", :title "Last Name"},
       :age {:type "number", :title "Age"}},
      :additionalProperties false,
      :required [:firstName :lastName :age]},
     :maxItems 1,
     :minItems 0,
     :title "Friend"}},
   :additionalProperties false,
   :required [:firstName :lastName]}

; https://jsfiddle.net/e7prdzkq/2/

```


___


### faq

_Q_: Is the library compatible with Clojure(Script)? 

_A_: Not yet. This library doesn't need anything jvm specific but the bijection
dependency would also need to be converted to Clojure(Script).


_Q_: Can I use this if I don't use react-jsonschema-form? 

_A_: Yes, please give it a try but your mileage may vary. In our experience consumers of
json-schema vary in their interpretation of the more advanced polymorphic dispatch features.


_Q_: What about the ui-schema component of react-jsonschema-form? How can I control the rendering order
and set additional display options?

_A_: This needs some hammock time. A lot of that information just isn't present in a prismatic
schema by default and part of the original goal of this conversion was to leverage prismatic schemas
as-is.


___

### license
This project is licensed under [MIT license](http://opensource.org/licenses/MIT).









