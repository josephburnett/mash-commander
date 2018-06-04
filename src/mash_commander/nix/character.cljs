(ns mash-commander.nix.character
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
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
                         :resting "35px"
                         :blinking "5px")]
        (dom/svg #js {:style #js {:width "500px"
                                  :height "800px"
                                  :float "right"}}
                 (dom/rect #js {:style #js {:x "5px"
                                            :y "35px"
                                            :width "450px"
                                            :height "700px"
                                            :rx "80px"
                                            :ry "80px"
                                            :stroke "black"
                                            :fill "white"
                                            :strokeWidth "8px"}})
                 (dom/rect #js {:style #js {:x "45px"
                                            :y "75px"
                                            :width "370px"
                                            :height "480px"
                                            :rx "45px"
                                            :ry "45px"
                                            :stroke "black"
                                            :fill "#f4f9ff"
                                            :strokeWidth "8px"}})
                 (dom/ellipse #js {:style #js {:cx "160px"
                                               :cy "210px"
                                               :rx "13px"
                                               :ry eye-height
                                               :fill "#0e3487"}})
                 (dom/ellipse #js {:style #js {:cx "295px"
                                               :cy "210px"
                                               :rx "13px"
                                               :ry eye-height
                                               :fill "#0e3487"}}))))
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
