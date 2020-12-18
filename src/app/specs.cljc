(ns app.specs
  (:require [clojure.spec.alpha :as s]
            [datascript.core :as d]))

(s/def ::age pos-int?)
(s/def ::id pos-int?)

(s/def ::description string?)
(s/def ::amount pos-int?)
(s/def ::delivery inst?)
(s/def ::tags (s/coll-of keyword? :into #{}))
(s/def ::item (s/keys :req-un [::description ::tags ::amount]))
(s/def ::items (s/map-of ::id ::item))
(s/def ::location (s/tuple double? double?))
(s/def ::order (s/keys :req-un [::id ::items ::delivery ::location]))

(def order
  {:id       123
   :items    {1 {:description "vadelmalimsa"
                 :tags        #{:good :red}
                 :amount      10}
              2 {:description "korvapuusti"
                 :tags        #{:raisin :sugar}
                 :amount      20}}
   :delivery #inst"2007-11-20T20:19:17.000-00:00"
   :location [61.499374 23.7408149]})

(comment

  (s/valid? ::order order)

  (let [schema {:aka {:db/cardinality :db.cardinality/many}}
        conn   (d/create-conn schema)]
    (d/transact! conn [ { :db/id -1
                         :name  "Maksim"
                         :age   45
                         :aka   ["Max Otto von Stierlitz", "Jack Ryan"] } ])
    (d/q '[ :find  ?n ?a
           :where [?e :aka "Max Otto von Stierlitz"]
           [?e :name ?n]
           [?e :age  ?a] ]
         @conn))

  (d/q '[:find  ?k ?x
         :in    [[?k [?min ?max]] ...] ?range
         :where
         [(?range ?min ?max) [?x ...]]
         [(even? ?x)] ]
       { :a [1 7], :b [2 4] }
       range)

  (d/q '[:find  ?u1 ?u2
         :in    $ %
         :where (follows ?u1 ?u2) ]
       [[1 :follows 2]
        [2 :follows 3]
        [3 :follows 4] ]
       '[[(follows ?e1 ?e2)
          [?e1 :follows ?e2]]
         [(follows ?e1 ?e2)
          [?e1 :follows ?t]
          (follows ?t ?e2)] ])

  (d/q '[:find ?color (max ?amount ?x) (min ?amount ?x)
         :in   [[?color ?x]] ?amount ]
       [[:red 10]  [:red 20] [:red 30] [:red 40] [:red 50]
        [:blue 7] [:blue 8]]
       3)

  'comment)
