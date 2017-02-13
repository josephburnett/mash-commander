(ns mash-commander.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! <! >!]]
            [clojure.string :as str]))

(enable-console-print!)

(defonce app-state
  (atom {:lines {:active {:state [:empty]
                          :letters [" " "!" "h" "s" "a" "m"]}
                 :history []}}))

(defn lines []
  (om/ref-cursor (:lines (om/root-cursor app-state))))

(defonce valid-words #{"hello" "world"})
(defonce valid-letters (set (concat (str/split "abcdefghijklmnopqrstuvwxyz" "")
                                    (str/split "ABCDEFGHIJKLMNOPQRSTUVWXYZ" "")
                                    (str/split "1234567890" ""))))

(defn recognize? [letters]
  (let [last-word (str/join (reverse (take-while #(not= " " %) letters)))]
    (contains? valid-words last-word)))

(defonce valid-commands #{"clear"})

(defmulti dispatch-enter
  (fn [cursor]
    (let [letters (get-in cursor [:active :letters])
          command (str/join (take-while #(not= " " %) (reverse letters)))]
      (if (contains? valid-commands command)
        command
        :default))))

(defmethod dispatch-enter :default
  [cursor]
  (as-> cursor c
    (assoc c :history (cons (:active c) (:history c)))
    (assoc c :active {:state [:empty] :letters []})))

(defmethod dispatch-enter "clear"
  [cursor]
  (as-> cursor c
    (assoc c :history [])
    (assoc c :active {:state [:empty] :letters []})))49

(defn handle-keydown [owner e]
  (let [key (.-key e)]
    (om/transact!
     (om/observe owner (lines))
     #(let [state (first (get-in % [:active :state]))]
        (cond
          ;; Typing a letter
          (contains? valid-letters key)
          (as-> % c
            (assoc-in c [:active :letters] (cons key (get-in c [:active :letters])))
            (if (recognize? (:letters c))
              (assoc-in c [:active :state] (cons :typing (get-in c [:active :state])))
              (assoc-in c [:active :state] (cons :mashing (get-in c [:active :state])))))
          ;; Ignore additional spaces
          (and (= " " key) (contains? #{:typing-space :mashing-space} state)) %
          ;; Pressing first space
          (= " " key)
          (as-> % c
            (assoc-in c [:active :letters] (cons key (get-in c [:active :letters])))
            (if (= :typing state)
              (assoc-in c [:active :state] (cons :typing-space (get-in c [:active :state])))
              (assoc-in c [:active :state] (cons :mashing-space (get-in c [:active :state])))))
          ;; Ignore backspace on empty
          (and (= "Backspace" key) (= :empty state)) %
          ;; Backspace
          (= "Backspace" key)
          (as-> % c
            (assoc-in c [:active :state] (rest (get-in c [:active :state])))
            (assoc-in c [:active :letters] (rest (get-in c [:active :letters]))))
          ;; Enter
          (= "Enter" key) (dispatch-enter %)
          ;; Ignore everything else
          :default %)))))

(defn line-view [cursor]
  (reify
    om/IInitState
    (init-state [_] {:focus false})
    om/IRenderState
    (render-state [_ state]
      (let [words (str/split (str/join (reverse (:letters cursor))) " ")
            spacing (contains? #{:typing-space :mashing-space} (first (:state cursor)))
            commanding (contains? valid-commands (first words))
            cursor-char (dom/span #js {:style #js {:color "#900"}} "\u2588")
            rendered-words (interpose
                            (dom/span nil " ")
                            (map #(if (contains? valid-words %)
                                    (dom/span #js {:style #js {:color "#0f0"}} %)
                                    (dom/span #js {:style #js {:color "#080"}} %))
                                 words))]
        (apply dom/div #js {:style #js {:fontSize "30px"
                                        :lineHeight "40px"
                                        :padding "15px 15px 0 15px"}}
               (as-> rendered-words r
                 (if (and (:focus state) spacing) (concat r [(dom/span nil " ") cursor-char]) r)
                 (if (and (:focus state) (not spacing)) (concat r [cursor-char]) r)
                 (if commanding (cons (dom/span #js {:style #js {:color "#33f"}} (first words))
                                      (rest r)) r)))))))

(defn app-view [cursor owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (set! (.-onkeydown js/document.body) (partial handle-keydown owner)))
    om/IRender
    (render [_]
      (apply dom/div #js {:style #js {:height "100vh"
                                      :width "100vw"
                                      :overflow "hidden"
                                      :padding "0"
                                      :margin "0"}}
             (cons
              (om/build line-view (get-in cursor [:lines :active]) {:state {:focus true}})
              (om/build-all line-view (get-in cursor [:lines :history])))))))

(om/root app-view app-state
         {:target (. js/document (getElementById "app"))})

