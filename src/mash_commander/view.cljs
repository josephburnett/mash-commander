(ns mash-commander.view
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [mash-commander.mode :as mode]
            [mash-commander.keyboard :as keyboard]
            [mash-commander.freestyle.freestyle-mode]
            [mash-commander.set.set-mode]
            [mash-commander.image :as image]
            [mash-commander.state :as state]
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
              (fn [e] (mode/dispatch-keydown cursor owner (.-key e))))
        (go-loop []
          (let [state (<! (om/get-shared owner :set-line))]
            (om/update! cursor state))
          (recur))))
    om/IWillReceiveProps
    (will-receive-props [_ next]
      (when-not (= (:mode cursor) (:mode next))
        (set! (.-onkeydown js/document.body)
              (fn [e] (mode/dispatch-keydown next owner (.-key e))))))
    om/IRenderState
    (render-state [_ state]
      (mode/line-render-state cursor owner state))))

(defn app-view [cursor owner]
  (reify
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
                (om/build keyboard/keyboard-view (get-in cursor [:lines]))
                (cons
                 (om/build line-view (get-in cursor [:lines :active]) {:state {:focus true}})
                 (om/build-all line-view (get-in cursor [:lines :history])))))))))

(defn init[]
  (om/root app-view state/app-state
           {:target (. js/document (getElementById "app"))
            :shared {:set-line (chan)}})
  (om/root image/display-view image/display-state
           {:target (. js/document (getElementById "display"))}))
  
