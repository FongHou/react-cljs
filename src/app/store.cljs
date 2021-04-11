(ns app.store
  (:require [cljsjs.use-store :refer [make-store]]))

(def init-state {:count 0})

(defmulti reducer (fn [_state [event]] event))

(let [[atom provider dispatch] (make-store reducer init-state)]
  (defonce store-atom atom)
  (defonce store-provider provider)
  (defonce use-dispatch dispatch))

(comment

  (require '[flow-storm.api :as fsa])

  (fsa/trace-ref store-atom)

  'comment)