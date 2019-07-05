(ns schema-forms.utils-test
  (:require [clojure.test :refer :all])
  (:require [schema-forms.utils :refer [title]]))

(deftest title-test
  (is (= "Counter Schema" (title "CounterSchema")))
  (is (= "GLOBAL_DOCUMENT_COUNTER" (title "GLOBAL_DOCUMENT_COUNTER")))
  (is (= "Play By Play" (title "play-by-play")))
  (is (= "Play By Play" (title :play-by-play)))
  (is (= "Testing Testing" (title (with-meta {} {:name "testingTesting"})))))
