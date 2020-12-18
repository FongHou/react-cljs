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
     (mf/fnc store
             [{:keys [children]}]
             (let [[store dispatch] (use-reducer reducer init-state)]
               (mf/use-effect
                (mf/deps store)
                #(reset! store-atom store))
               [:> (mf/provider dispatch-ctx) {:value dispatch}
                children]))]))

(defn use-atom
  [reducer init-state]
  (let [dispatch-ctx (mf/create-context)
        _ (unchecked-set dispatch-ctx "displayName" "dispatch")
        use-dispatch (fn use-dispatch []
                       (mf/use-ctx dispatch-ctx))
        [atom provider] (make-provider reducer
                                       init-state
                                       dispatch-ctx)]
    [atom provider use-dispatch]))

(defn derived-atom
  ([refs key f]
   (derived-atom refs key f {}))
  ([refs key f opts]
   (let [{:keys [ref check-equals?]
          :or {check-equals? true}} opts
         recalc (case (count refs)
                  1 (let [[a] refs] #(f @a))
                  2 (let [[a b] refs] #(f @a @b))
                  3 (let [[a b c] refs] #(f @a @b @c))
                  #(apply f (map deref refs)))
         sink   (if ref
                  (doto ref (reset! (recalc)))
                  (atom (recalc)))
         watch  (if check-equals?
                  (fn [_ _ _ _]
                    (let [new-val (recalc)]
                      (when (not= @sink new-val)
                        (reset! sink new-val))))
                  (fn [_ _ _ _]
                    (reset! sink (recalc))))]
     (doseq [ref refs]
       (add-watch ref key watch))
     sink)))