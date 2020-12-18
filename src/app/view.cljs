(ns app.view
  (:require
   [applied-science.js-interop :as j]
   [cljsjs.console :refer [spy log]]
   [clojure.spec.alpha :as s]
   [com.fulcrologic.guardrails.core :refer [>defn >def | ? =>]]
   [com.wsscode.edn-json :refer [edn->json json->edn]]
   [cuerdas.core :as strx]
   [medley.core :as cljx]
   [goog.dom :as dom]
   [httpurr.client :as http]
   [httpurr.client.xhr :refer [client]]
   [okulary.core :as ol]
   [odoyle.rules :as o :refer-macros [ruleset]]
   [oops.core :refer [oget oset! ocall]]
   [promesa.core :as p]
   [rumext.alpha :as mf]
   #_["@aws-amplify/ui-react" :refer [withAuthenticator AmplifySignout]]
   ["react-router-dom" :refer [HashRouter Routes Route Link NavLink Outlet]]
   ["react-hook-form" :refer [useForm]]
   ["react-query" :refer [useQuery]]
   ["react-table" :refer [useTable useSortBy]]
   [app.login :as login]
   [app.store :as app :refer [reducer]]))

(set! *warn-on-infer* true)

;; useRef, useLayoutEffect
(mf/defc textarea
  [{:keys [value]}]
  (let [ref   (mf/use-ref)
        state (mf/use-state 0)]
    (mf/use-layout-effect
     (mf/deps value)
     (fn []
       (let [^js node (mf/ref-val ref)]
         (oset! node [:style :height] "0")
         (oset! node [:style :height]
                (str (+ 2 (oget node :scrollHeight)) "px")))))
    [:textarea
     {:ref           ref
      :style         {:width   "100%"
                      :padding "10px"
                      :font    "inherit"
                      :outline "none"
                      :resize  "none"}
      :default-value value
      :placeholder   "Auto-resizing textarea"
      :on-change     (fn [_] (swap! state inc))}]))

(defmethod reducer ::timer
  [state [_ data]]
  (assoc state ::timer data))

(def timer (ol/atom 0))
(js/setInterval #(swap! timer inc) 160)

(mf/defc global-timer
  [props]
  (let [ts (mf/deref timer)
        dispatch (app/use-dispatch)]
    (mf/use-effect
     #js[ts]
     #(dispatch [::timer ts]))
    [:div (props :children) ts]))

;; useState, useEffect
(mf/defc local-timer
  [props]
  (let [local (mf/use-state 0)]
    (mf/use-effect
     (fn []
       (let [sem (js/setInterval #(swap! local inc) 1000)]
         #(js/clearInterval sem))))
    [:div
     [:div "Timer (global): " (mf/deref timer)]
     [:div "Timer (store): " (::timer (mf/deref app/store-atom))]
     [:div (props :children) @local]]))

;; -----------------------------------------------------------------------------
;; use-store

(def count-atom (ol/derived :count app/store-atom))

(mf/defc button
  {::mf/wrap [#(mf/memo % =)]}
  [{:keys [text on-click]}]
  (let [down (mf/use-state false)
        dispatch (app/use-dispatch)]
    [:button.ui.primary.button
     {:on-mouse-down #(do (reset! down true))
      :on-mouse-up   #(reset! down false)
      :on-click      #(dispatch on-click)}
     (if @down
       (str "~" text "~")
       text)]))

(mf/defc show []
  [:div.ui.label (str (mf/deref count-atom))])

(derive :count/up ::counter)
(derive :count/down ::counter)
(derive :count/reset ::counter)

#ztrace
(defmethod reducer ::counter
  [state [event & args]]
  (case event
    :count/reset
    (assoc state :count (first args))
    :count/up
    (update state :count inc)
    :count/down
    (update state :count dec)
    state))

(mf/defc counter
  []
  (let [dispatch (app/use-dispatch)]
    (mf/use-effect
     #(dispatch [:count/reset 0]))
    [:div.ui.container
     [:& button {:text "-" :on-click [:count/down]}]
     [:& show]
     [:& button {:text "+" :on-click [:count/up]}]
     [:div.ui.divider]
     [:& global-timer nil "Timer: "]]))

;; ---------------------------------------------------------------------------

(defn decode
  [response]
  (update response :body #(json->edn (js/JSON.parse %))))

(defn encode
  [request]
  (update request :body #(js/JSON.stringify (edn->json %))))

(defn fetch [url]
  (-> (http/get client url) (p/then decode)))

(def rq-opts #js {:refetchOnWindowFocus false})

(mf/defc trow
  [{:keys [row]}]
  {::mf/wrap [mf/memo]}
  [:> :tr (ocall row :getRowProps)
   (for [cell (oget row :cells)]
     [:> :td (ocall cell :getCellProps)
      (ocall cell :render "Cell")])])

(mf/defc thead
  [{:keys [tdata children]}]
  [:thead
   (for [tr (oget tdata :headerGroups)]
     [:> :tr (ocall tr :getHeaderGroupProps)
      (for [col (oget tr :headers)]
        [:> :th (ocall col :getHeaderProps
                       (ocall col :getSortByToggleProps))
         (ocall col :render "Header")
         (if (fn? children)
           (children {:header col})
           children)])])])

(defn fetch-rates []
  (fetch "https://api.ratesapi.io/api/latest"))

(defmethod reducer ::rates-loaded
  [state [_ data]]
  (assoc state ::rates data))

(mf/defc my-table []
  (let [resp (useQuery "currency" fetch-rates)
        rates (get-in resp ["data" :body])
        columns (mf/use-memo #(j/lit [{:Header   "Base"
                                       :accessor "base"
                                       :sortType "basic"}
                                      {:Header   "Rates"
                                       :accessor "rates"
                                       :sortType "basic"}]))
        data (mf/use-memo #(j/lit [{:base  "EUR"
                                    :rates "USD 10"}
                                   {:base  "USD"
                                    :rates "YEN 100"}]))
        tdata (useTable #js{:columns columns
                            :data    data}
                        useSortBy)
        dispatch (app/use-dispatch)]
    (mf/use-effect
     #(dispatch [::rates-loaded rates]))
    (if (oget resp :isLoading)
      [:h3.ui.header "Loading..."]
      [:> :table (-> (ocall tdata :getTableProps)
                     (j/assoc! :className "ui unstackable 
                                           compact small
                                           selectable
                                           celled table"))
       [:& thead {:tdata tdata}
        (fn [{:keys [header]}]
          [:i {:class
               (if (oget header :isSorted)
                 (if (oget header :isSortedDesc)
                   "arrow down icon"
                   "arrow up icon")
                 "")}])]
       [:> :tbody (ocall tdata :getTableBodyProps)
        (for [row (oget tdata :rows)]
          (do (ocall tdata :prepareRow row)
              [:& trow {:row row}]))]])))

(mf/defc timers []
  [:div.ui.container
   [:> Outlet nil]])

(mf/defc my-form
  []
  (let [{:keys [register watch errors handleSubmit]} (j/lookup (useForm))
        on-submit (fn [data] (log "on-submit" data))]
    [:form.ui.form.warning
     {:on-submit (handleSubmit on-submit)}
     [:div.inline.field
      [:label "First Name"]
      [:input {:type "text"
               :name "example"
               :default-value "test"
               :placeholder "test"
               :ref (register)}]]
     [:div.required.inline.field {:data-tooltip "This is required."
                                  :data-position "bottom left"}
      [:label "Last Name"]
      [:input {:name "exampleRequired"
               :placeholder "required"
               :ref (register #js{:required true})}]]
     (when (j/contains? errors "exampleRequired")
       [:div.ui.warning.message "This is required."])
     [:button.ui.button {:type "submit"} "Submit"]]))

;; React Router
(mf/defc nav-bar []
  [:nav.ui.breadcrumb
   [:> NavLink {:to "counter" :class "section"}
    "Counter"]
   [:div.divider "|"]
   [:> NavLink {:to "table" :class "section"}
    "Table"]
   [:div.divider "|"]
   [:> NavLink {:to "form" :class "section"}
    "Form"]
   #_[:> NavLink {:to "timer/local" :class "section"}
    "Timer (local)"]
   #_[:> NavLink {:to "timer/global" :class "section"}
    "Timer (global)"]])

(def $e mf/element)

(mf/defc router []
  [:div.ui.container
   [:& nav-bar]
   [:div.ui.divider]
   [:> Routes nil
    [:> Route {:path "counter" :element ($e counter)}]
    [:> Route {:path "table" :element ($e my-table)}]
    [:> Route {:path "form" :element ($e my-form)}]
    [:> Route {:path "timer" :element ($e timers)}
     [:> Route {:path "local"
                :element (mf/html [:> local-timer nil "Timer (local): "])}]
     [:> Route {:path "global"
                :element (mf/html [:> global-timer nil "Timer (global): "])}]]]])

(mf/defc root []
  {:mf/wrap [#(mf/catch % {:on-error js/console.log})]}
  [:& app/store-provider nil
   [:& HashRouter nil
    [:& router]]])

#_(mf/defc amplify []
  (let [ui (withAuthenticator root)]
    (js/console.log ui)
    [:& ui]))

(defonce auth-result (atom nil))

(defn init []
  (some->>
   (dom/getElement "root")
   (mf/mount (mf/element root))))

(def rules
  (ruleset
   {::print-time
    [:what
     [::time ::total tt]
     :then
     (println tt)]

    ::move-player
    [:what
     [::time ::total tt]
     [::time ::delta dt]
     [::player ::x x {:then false}]
     [::player ::y y {:then false}]
     :then
     (-> o/*session*
         (o/insert ::player ::y (+ y tt))
         (o/insert ::player ::x (+ x dt))
         o/reset!)] 

    ::get-player
    [:what
     [::player ::x x]
     [::player ::y y]]
    
    }))

(def *session
  (atom (reduce o/add-rule (o/->session) rules)))


(comment

  (require '[flow-storm.api :as fsa]) 

  (swap! *session
         (fn [session]
           (-> session
               (o/insert ::time {::total 100
                                 ::delta 0.1})
               (o/insert ::player {::x 20 ::y 15})
               o/fire-rules)))

  (println (o/query-all @*session ::get-player))

  (fsa/trace-ref *session)

  (spy @app/store-atom)

  (mf/unmount (dom/getElement "root"))

  (spy (js/firebase.app))
  (spy (js/firebase.firestore))
  (spy (js/firebase.database))

  (-> (spy @login/auth-result)
      (ocall ".user.toJSON")
      json->edn spy)

  (oget @login/auth-result ".credential.accessToken")

  (-> @login/auth-result
      (ocall ".credential.toJSON")
      (json->edn)
      spy)
  
  (-> (fetch "http://localhost:3000/actor?select=*,film(title,description)")
      (p/chain js/console.log #(spy %)))


  'comment)
