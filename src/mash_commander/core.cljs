(ns mash-commander.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! close! <! >!]]
            [clojure.string :as str]
            [ajax.core :refer [GET]]
            [mash-commander.command :as command]
            [mash-commander.speech :as speech]
            [mash-commander.wiki]
            [mash-commander.wolfram]))

(enable-console-print!)

(defonce app-state
  (atom {:lines {:active {:state [:empty]
                          :letters []}
                 :history []}
         :words {}}))

(defn lines []
  (om/ref-cursor (:lines (om/root-cursor app-state))))

(defn words []
  (om/ref-cursor (:words (om/root-cursor app-state))))

(def valid-letters (set (concat (str/split "abcdefghijklmnopqrstuvwxyz" "")
                                    (str/split "ABCDEFGHIJKLMNOPQRSTUVWXYZ" "")
                                    (str/split "1234567890" ""))))

(defn last-word [letters]
  (as-> letters l
    (if (and (not (empty? letters)) (= " " (first letters)))
      (rest l) l)
    (str/join (reverse (take-while #(not= " " %) l)))))

(defn recognize? [owner word]
  (let [trie (om/observe owner (words))
        letters (seq word)]
    (= "" (get-in trie (conj (into [] (map str/lower-case letters)) "")))))

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
            (if (recognize? owner (last-word (get-in c [:active :letters])))
              (assoc-in c [:active :state] (cons :typing (get-in c [:active :state])))
              (assoc-in c [:active :state] (cons :mashing (get-in c [:active :state])))))
          ;; Ignore leading and additional spaces
          (and (= " " key) (contains? #{:empty :typing-space :mashing-space} state)) %
          ;; Pressing first space
          (= " " key)
          (as-> % c
            (assoc-in c [:active :letters] (cons key (get-in c [:active :letters])))
            (if (= :typing state)
              (do
                (let [what (last-word (get-in c [:active :letters]))]
                  (go (>! speech/say what)))
                (assoc-in c [:active :state] (cons :typing-space (get-in c [:active :state]))))
              (assoc-in c [:active :state] (cons :mashing-space (get-in c [:active :state])))))
          ;; Ignore backspace on empty
          (and (= "Backspace" key) (= :empty state)) %
          ;; Backspace
          (= "Backspace" key)
          (as-> % c
            (assoc-in c [:active :state] (rest (get-in c [:active :state])))
            (assoc-in c [:active :letters] (rest (get-in c [:active :letters]))))
          ;; Enter
          (= "Enter" key)
          (do
            (when (= :typing state)
              (let [what (last-word (get-in % [:active :letters]))]
                (go (>! speech/say what))))
            (command/dispatch-enter %))
          ;; Ignore everything else
          :default %)))))

(defn line-view [cursor owner]
  (reify
    om/IInitState
    (init-state [_] {:focus false})
    om/IDidMount
    (did-mount [_]
      (when (om/get-state owner :focus)
        (go-loop []
          (let [state (<! (om/get-shared owner :set-line))]
            (om/update! cursor state))
          (recur))))
    om/IRenderState
    (render-state [_ state]
      (let [words (str/split (str/join (reverse (:letters cursor))) " ")
            spacing (contains? #{:typing-space :mashing-space} (first (:state cursor)))
            commanding (contains? @command/valid-commands (first words))
            command-prompt (dom/span #js {:style #js {:color "#33f"
                                                      :fontWeight "bold"}} "$ ")
            cursor-char (dom/span #js {:style #js {:color "#900"}} "\u2588")
            rendered-words (interpose
                            (dom/span nil " ")
                            (map #(if (recognize? owner %)
                                    (dom/span #js {:style #js {:color "#0f0"}} %)
                                    (dom/span #js {:style #js {:color "#080"}} %))
                                 words))]
        (apply dom/div #js {:style #js {:fontSize "30px"
                                        :lineHeight "40px"
                                        :padding "15px 15px 0 15px"}
                            :onClick #(when-not (:focus state) (go (>! (om/get-shared owner :set-line) cursor)))}
               (as-> rendered-words r
                 (if (and (:focus state) spacing) (concat r [(dom/span nil " ") cursor-char]) r)
                 (if (and (:focus state) (not spacing)) (concat r [cursor-char]) r)
                 (if commanding (cons (dom/span #js {:style #js {:color "#33f"}} (first words))
                                      (rest r)) r)
                 (if (:focus state) (cons command-prompt r) r)))))))


(defn app-view [cursor owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (set! (.-onkeydown js/document.body) (partial handle-keydown owner))
      (go (GET "/words.json"
               {:params {:response-format :json
                         :keywords? false}
                :handler (fn [trie]
                           (om/transact! (om/observe owner (words)) #(merge % trie)))
                :error-handler print})))
    om/IRender
    (render [_]
      (let [set-state (chan)]
        (apply dom/div #js {:style #js {:height "100vh"
                                        :width "100vw"
                                        :overflow "hidden"
                                        :padding "0"
                                        :margin "0"}}
               (cons
                (om/build line-view (get-in cursor [:lines :active]) {:state {:focus true}})
                (om/build-all line-view (get-in cursor [:lines :history]))))))))

(om/root app-view app-state
         {:target (. js/document (getElementById "app"))
          :shared {:set-line (chan)}})

