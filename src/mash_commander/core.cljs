(ns mash-commander.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! <! >!]]
            [clojure.string :as str]))

(enable-console-print!)

(defonce app-state
  (atom {:line {:state [:empty]
                :letters [" " "!" "h" "s" "a" "m"]}}))

(defn line []
  (om/ref-cursor (:line (om/root-cursor app-state))))

(defonce valid-words #{"hello" "world"})
(defonce valid-letters (set (str/split "abcdefghijklmnopqrstuvwyz" "")))

(defn recognize? [letters]
  (let [last-word (str/join (reverse (take-while #(not= " " %) letters)))]
    (contains? valid-words last-word)))

(defn handle-keydown [owner e]
  (let [key (.-key e)]
    (om/transact!
     (om/observe owner (line))
     #(let [state (first (:state %))]
        (cond
          ;; Typing a letter
          (contains? valid-letters key)
          (as-> % c
            (assoc c :letters (cons key (:letters %)))
            (if (recognize? (:letters c))
              (assoc c :state (cons :typing (:state %)))
              (assoc c :state (cons :mashing (:state %)))))
          ;; Ignore additional spaces
          (and (= " " key) (contains? #{:typing-space :mashing-space} state)) %
          ;; Pressing first space
          (= " " key)
          (as-> % c
            (assoc c :letters (cons key (:letters %)))
            (if (= :typing (first (:state %)))
              (assoc c :state (cons :typing-space (:state %)))
              (assoc c :state (cons :mashing-space (:state %)))))
          ;; Ignore backspace on empty
          (and (= "Backspace" key) (= :empty state)) %
          ;; Backspace
          (= "Backspace" key)
          (as-> % c
            (assoc c :state (rest (:state c)))
            (assoc c :letters (rest (:letters c))))
          ;; Ignore everything else
          :default %)))))

(defn line-view [cursor]
  (reify
    om/IRender
    (render [_]
      (let [words (str/split (str/join (reverse (:letters cursor))) " ")]
        (apply dom/div nil
               (interleave
                (map #(if (contains? valid-words %)
                        (dom/span #js {:style #js {:color "#c00"}} %)
                        (dom/span nil %))
                     words)
                (repeat (dom/span nil " "))))))))

(defn app-view [cursor owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (set! (.-onkeydown js/document.body) (partial handle-keydown owner)))
    om/IRender
    (render [_]
      (dom/div #js {:style #js {:height "100vh"
                                :width "100vw"
                                :overflow "hidden"
                                :padding "0"
                                :margin "0"}}
               (om/build line-view (:line cursor))))))

(om/root app-view app-state
         {:target (. js/document (getElementById "app"))})

