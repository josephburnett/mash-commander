(ns mash-commander.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! <! >!]]
            [clojure.string :as str]))

(enable-console-print!)

(defonce valid-words #{"hello" "world"})
(defonce valid-letters (set (str/split "abcdefghijklmnopqrstuvwyz" "")))

(defn recognize? [letters]
  (let [last-word (last (str/split (str/join "" letters) " "))]
    (contains? valid-words last-word)))

(defonce app-state
  (atom {:line {:input-state-stack [:empty]
                :letters ["m" "a" "s" "h" "!" " "]}}))

(defn line []
  (om/ref-cursor (:line (om/root-cursor app-state))))

(defn handle-keydown [owner e]
  (let [key (.-key e)]
    (om/transact! (om/observe owner (line))
                  #(let [state (first (:input-state-stack %))
                         new-state
                         (cond
                           ;; Typing a letter
                           (contains? valid-letters key)
                           (as-> % c
                             (assoc c :letters (conj (:letters %) key))
                             (if (recognize? (:letters c))
                               (assoc c :input-state-stack (cons :typing (:input-state-stack %)))
                               (assoc c :input-state-stack (cons :mashing (:input-state-stack %)))))
                           ;; Pressing additional spaces
                           (and (= " " key) (contains? #{:typing-space :mashing-space} state)) %
                           ;; Pressing first space
                           (= " " key)
                           (as-> % c
                             (assoc c :letters (conj (:letters %) key))
                             (if (= :typing (first (:input-state-stack %)))
                               (assoc c :input-state-stack (cons :typing-space (:input-state-stack %)))
                               (assoc c :input-state-stack (cons :mashing-space (:input-state-stack %)))))
                           :default %)]
                     (print "state:" new-state)
                     new-state))))

(defn line-view [cursor]
  (reify
    om/IRender
    (render [_]
      (let [words (str/split (str/join "" (:letters cursor)) " ")]
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

