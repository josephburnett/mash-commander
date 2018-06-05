(ns mash-commander.nix.character
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [mash-commander.nix.story :as story]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! close! <! >! timeout]]))

(defonce state-chan (chan))

(defn view [cursor owner]
  (reify
    om/IInitState
    (init-state [_] {})
    om/IRenderState
    (render-state [_ state]
      (let [state (:state cursor)
            eye-height (condp = state
                         :resting "2.1vh"
                         :blinking ".3vh")]
        (dom/div nil
                 (dom/svg #js {:style #js {:width "30.0vh"
                                           :height "48.0vh"
                                           :float "right"}}
                          (dom/rect #js {:style #js {:x ".3.vh"
                                                     :y "2.1vh"
                                                     :width "27.0vh"
                                                     :height "42.0vh"
                                                     :rx "4.8vh"
                                                     :ry "4.8vh"
                                                     :stroke "black"
                                                     :fill "white"
                                                     :strokeWidth ".5vh"}})
                          (dom/rect #js {:style #js {:x "2.7vh"
                                                     :y "4.5vh"
                                                     :width "22.0vh"
                                                     :height "28.0vh"
                                                     :rx "2.7vh"
                                                     :ry "2.7vh"
                                                     :stroke "black"
                                                     :fill "#f4f9ff"
                                                     :strokeWidth ".5vh"}})
                          (dom/ellipse #js {:style #js {:cx "9.6vh"
                                                        :cy "12.6vh"
                                                        :rx ".8vh"
                                                        :ry eye-height
                                                        :fill "#0e3487"}})
                          (dom/ellipse #js {:style #js {:cx "17.7vh"
                                                        :cy "12.6vh"
                                                        :rx ".8vh"
                                                        :ry eye-height
                                                        :fill "#0e3487"}}))
                 (om/build story/component (:page cursor)))))
    om/IDidMount
    (did-mount [_]
      (go-loop []
        (let [state (<! state-chan)]
          (om/transact! cursor #(assoc % :state state)))
        (recur))
      (go-loop []
        (<! (timeout 500))
        (>! state-chan :blinking)
        (<! (timeout 150))
        (>! state-chan :resting)
        ;; blink every 2-5 seconds
        (<! (timeout (+ (rand-int 3000) 2000)))
        (recur)))))
