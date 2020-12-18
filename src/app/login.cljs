(ns app.login
  (:require [cljsjs.console :refer [spy]]
            [oops.core :refer [oget oset! ocall]])) 

(defn send-login-event
  [auth-result]
  (let [user (ocall auth-result ".user.toJSON")
        event (js/CustomEvent. "oauth-event" #js{:detail user})]
    (spy event "send-login-event")
    (js/window.dispatchEvent event)))

(defonce auth-result (atom nil))

(defn ^:export get-auth-result []
  @auth-result)

(defn init []
  (when-not @auth-result
   (js/firebaseAuthUI.start 
    "#firebaseui-auth-container"
    #js {:signInOptions #js[js/firebase.auth.GithubAuthProvider.PROVIDER_ID]
         :signInFlow "popup"
         :callbacks #js{:signInSuccessWithAuthResult
                        (fn [result redirect]
                          (reset! auth-result result)
                          (spy redirect "redirect...")
                          (spy (send-login-event result))
                          false)}})))