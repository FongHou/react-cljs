(ns app.preload
  (:require [flow-storm.api :as fsa]
            [app.store :as app]))

(set! *warn-on-infer* true)

(fsa/connect)

(fsa/trace-ref app/store-atom)