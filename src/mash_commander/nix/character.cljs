(ns mash-commander.nix.character
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [mash-commander.speech :as speech]
            [mash-commander.state :as state]
            [mash-commander.nix.story :as story]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [pub sub chan put! close! <! >! timeout]]))

(defonce state-chan (chan))

(def event-chan (chan))
(def event-pub (pub event-chan :type))

(defn wait-event [e]
  (let [done (chan)
        event-sub (chan)
        test (if (contains? e :test)
               (:test e)
               #(= e %))]
    (sub event-pub (:type e) event-sub)
    (go-loop []
      (let [event (<! event-sub)]
        (if (test event)
          (do
            (close! event-sub)
            (close! done))
          (recur)))
      done)))
    
(defn run-page [cursor page]
  (let [done (chan)]
    (go
      (when (:say page)
        (om/transact! (state/lines)
                      #(let [new-line {:mode :nix
                                       :result (:say page)}]
                         (assoc % :history (cons new-line (:history %)))))
        (<! (speech/wait-say (:say page))))
      (when (:wait-event page)
        (<! (wait-event (:wait-event page))))
      (when (:then page)
        (<! (run-page cursor (:then page))))
      (when (:goto page)
        (let [new-page (:goto page)]
          (om/transact! cursor #(assoc % :current-page (:goto page))))
        (close! done))
    done)))

(defn story-component [cursor]
  (reify
    om/IRender
    (render [_]
      (let [page (get story/pages (:current-page cursor))]
        (run-page cursor page))
      nil)))
          
(defn view [cursor owner]
  (reify
    om/IInitState
    (init-state [_] {})
    om/IRenderState
    (render-state [_ state]
      (let [state (get-in cursor [:appearance :state])
            eye-height (condp = state
                         :resting "21"
                         :blinking "3")
            color (condp = (get-in cursor [:appearance :color])
                    "white" "#fff"
                    "blue" "#00f"
                    "red" "#f00"
                    "green" "#0f0")]
        (dom/div nil
                 ;; Nix character
                 (dom/svg #js {:style #js {:width "30.0vh"
                                           :height "100.0vh"
                                           :float "right"
                                           :padding "5vh"}
                               :viewBox "0 0 300 1000"}
                          (dom/rect #js {:x "5"
                                         :y "5"
                                         :width "295"
                                         :height "475"
                                         :rx "48"
                                         :ry "48"
                                         :stroke "black"
                                         :fill color
                                         :strokeWidth "5"})
                          (dom/rect #js {:x "30"
                                         :y "30"
                                         :width "245"
                                         :height "345"
                                         :rx "27"
                                         :ry "27"
                                         :stroke "black"
                                         :fill "#f4f9ff"
                                         :strokeWidth "5"})
                          (dom/ellipse #js {:cx "105"
                                            :cy "126"
                                            :rx "8"
                                            :ry eye-height
                                            :fill "#0e3487"})
                          (dom/ellipse #js {:cx "195"
                                            :cy "126"
                                            :rx "8"
                                            :ry eye-height
                                            :fill "#0e3487"})
                          (dom/rect #js {:x "130"
                                         :y "482"
                                         :width "40"
                                         :height "20"
                                         :fill "#fff"})
                          (dom/circle #js {:cx "150"
                                           :cy "502"
                                           :r "20"
                                           :fill "#fff"})
                          (dom/rect #js {:x "145"
                                         :y "500"
                                         :width "10"
                                         :height "500"
                                         :fill "#fff"}))
                 ;; Keyboard cable
                 (dom/svg #js {:style #js {:position "absolute"
                                           :bottom "0vh"
                                           :width "100vh"
                                           :height "7vh"}
                               :viewBox "0 0 1000 70"}
                          (dom/rect #js {:x "455"
                                         :y "0"
                                         :width "10"
                                         :height "100"
                                         :fill "#fff"})
                          (dom/rect #js {:x "440"
                                         :y "0"
                                         :width "40"
                                         :height "20"
                                         :fill "#fff"})
                          (dom/circle #js {:cx "460"
                                           :cy "20"
                                           :r "20"
                                           :fill "#fff"}))
                 ;; Story telling
                 (om/build story-component (:page cursor)))))
    om/IDidMount
    (did-mount [_]
      (go-loop []
        (let [state (<! state-chan)]
          (om/transact! cursor #(assoc-in % [:appearance :state] state)))
        (recur))
      (go-loop []
        (<! (timeout 500))
        (>! state-chan :blinking)
        (<! (timeout 150))
        (>! state-chan :resting)
        ;; blink every 2-5 seconds
        (<! (timeout (+ (rand-int 3000) 2000)))
        (recur)))))
