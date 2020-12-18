(ns app.core
  (:require [app.login]
            [app.view]
            #_["aws-amplify" :refer [Auth] :default Amplify]
            #_["/aws-exports" :default awsmobile]))

(defn ^:dev/after-load ^:export main []
  ;; (.configure Amplify awsmobile)
  ;; (js/console.log awsmobile)
  ;; (js/console.log Amplify)
  (app.view/init))

(comment
  'comment)