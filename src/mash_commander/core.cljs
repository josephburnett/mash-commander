(ns mash-commander.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! <! >!]]
            [clojure.string]))

(enable-console-print!)

(defonce app-state
  (atom {:line ["m" "a" "s" "h" "!" " "]}))

(defn line []
  (om/ref-cursor (:line (om/root-cursor app-state))))

(defn line-view [cursor]
  (reify
    om/IRender
    (render [_]
      (dom/div nil (clojure.string/join "" (:line cursor))))))

(defn app-view [cursor owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (set! (.-onkeydown js/document.body)
            (fn [e]
              (let [l (om/observe owner (line))]
                (om/transact! l #(conj % (.-key e)))))))
    om/IRender
    (render [_]
      (dom/div #js {:style #js {:height "100vh"
                                :width "100vw"
                                :overflow "hidden"
                                :padding "0"
                                :margin "0"}}
               (om/build line-view cursor)))))

(om/root app-view app-state
         {:target (. js/document (getElementById "app"))})

