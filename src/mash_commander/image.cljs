(ns mash-commander.image
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! close! <! >!]]))

(def show (chan))

(def display-state (atom {:sequence 0}))

(defn- display-clear [cursor sequence]
  (om/transact! cursor #(if (= sequence (:sequence %)) (dissoc % :showing-image-url) %)))

(defn display-view [cursor owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (go-loop []
        (let [what (<! show)]
          (om/transact!
           cursor
           #(let [sequence (inc (:sequence %))]
              (js/setTimeout (partial display-clear cursor sequence) 3000)
              (as-> % c
                (assoc c :sequence sequence)
                (assoc c :showing-image-url what)))))
        (recur)))
    om/IRender
    (render [_]
      (let [image-url (get-in cursor [:showing-image-url])]
        (dom/div #js {:style #js {:height "100vh"
                                  :width "100vw"
                                  :overflow "hidden"
                                  :padding "0"
                                  :margin "0"
                                  :position "absolute"}}
                 (dom/div #js {:style #js {:height "40vh"
                                           :width "50vw"
                                           :maxHeight "100%"
                                           :padding "0"
                                           :margin "15vh auto 0 auto"
                                           :textAlign "center"}}
                          (when-not (nil? image-url)
                            (dom/img #js {:src image-url
                                          :className "display"}))))))))
                          
