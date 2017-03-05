(ns mash-commander.view
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [mash-commander.mode :as mode]
            [mash-commander.set :as set]
            [ajax.core :refer [GET]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! close! <! >!]]))

(defn line-view [cursor owner]
  (reify
    om/IInitState
    (init-state [_] {:focus false})
    om/IDidMount
    (did-mount [_]
      (when (om/get-state owner :focus)
        (set! (.-onkeydown js/document.body)
              (fn [e] (mode/dispatch-keydown cursor owner e)))
        (go-loop []
          (let [state (<! (om/get-shared owner :set-line))]
            (om/update! cursor state))
          (recur))))
    om/IWillReceiveProps
    (will-receive-props [_ next]
      (when-not (= (:mode cursor) (:mode next))
        (set! (.-onkeydown js/document.body)
              (fn [e] (mode/dispatch-keydown next owner e)))))
    om/IRenderState
    (render-state [_ state]
      (mode/line-render-state cursor owner state))))

(defn- load-words [cursor]
  (go (GET "/words.json"
           {:params {:response-format :json
                     :keywords? false}
            :handler (fn [trie]
                       (om/transact! cursor :words #(merge % trie)))
            :error-handler print})))

(defn- load-set [cursor name]
  (go (GET (str "/sets/" name ".json")
           {:params {:response-format :json
                     :keywords? false}
            :handler (fn [s]
                       (om/transact! cursor :sets #(assoc % name (set/load s))))
            :error-handler print})))

(defn app-view [cursor owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (load-words cursor)
      (load-set cursor "animals"))
    om/IRender
    (render [_]
      (let [set-state (chan)]
        (apply dom/div #js {:style #js {:height "100vh"
                                        :width "100vw"
                                        :overflow "hidden"
                                        :padding "0"
                                        :margin "0"
                                        :position "absolute"}}
               (cons
                (om/build line-view (get-in cursor [:lines :active]) {:state {:focus true}})
                (om/build-all line-view (get-in cursor [:lines :history]))))))))

