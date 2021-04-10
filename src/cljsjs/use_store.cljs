(ns cljsjs.use-store
  (:require
   [react :refer [useReducer]]
   [okulary.core :as ol]
   [rumext.alpha :as mf]))

(defn use-reducer
  ([reducer init-state]
   (use-reducer reducer init-state js/undefined))
  ([reducer init-state init-fn]
   (useReducer
    (if (and (not (fn? reducer)) (ifn? reducer))
      (mf/use-fn (mf/deps reducer)
                 (fn wrap-reducer [state event]
                   (reducer state event)))
      reducer)
    init-state
    init-fn)))

(defn- make-provider
  [reducer init-state dispatch-ctx]
  (let [store-atom (ol/atom nil)
        store-ro-atom (ol/derived identity store-atom)]
    [store-ro-atom
     (mf/fnc store-component
             [{:keys [children]}]
             (let [[store dispatch] (use-reducer reducer init-state)]
               (mf/use-layout-effect
                (mf/deps store)
                #(reset! store-atom store))
               [:> (mf/provider dispatch-ctx) {:value dispatch}
                children]))]))

(defn use-atom
  [reducer init-state]
  (let [dispatch-ctx (mf/create-context)
        _ (unchecked-set dispatch-ctx "displayName" "store-dispatch")
        use-dispatch (fn use-dispatch []
                       (mf/use-ctx dispatch-ctx))
        [atom provider] (make-provider reducer
                                       init-state
                                       dispatch-ctx)]
    [atom provider use-dispatch]))