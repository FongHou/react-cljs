(ns app.store
  (:require
   [clojure.spec.alpha :as s]
   [cljsjs.use-store :refer [use-atom derived-atom]]
   [okulary.core :as ol]))

(def init-state {:count 0})

(defmulti reducer (fn [_state [event]] event))

(let [[atom provider use-dispatch]
      (use-atom reducer init-state)]
  (def store-atom atom)
  (def store-provider provider)
  (def use-dispatch use-dispatch))


(comment

  (require '[flow-storm.api :as fsa]) 

  (fsa/trace-ref store-atom)

  'comment)