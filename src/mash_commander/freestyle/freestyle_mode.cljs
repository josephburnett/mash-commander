(ns mash-commander.freestyle.freestyle-mode
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [mash-commander.freestyle.command :as command]
            [mash-commander.mode :as mode]
            [mash-commander.state :as mash-state]
            [mash-commander.set.manifest :as set-manifest]
            [mash-commander.speech :as speech]
            [mash-commander.freestyle.wiki :as wiki]
            [mash-commander.freestyle.wolfram :as wolfram]
            [mash-commander.words :as mash-words]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as str]
            [cljs.core.async :refer [chan put! close! <! >!]]))

(def ^:private valid-letters
  (set (concat (str/split "abcdefghijklmnopqrstuvwxyz" "")
               (str/split "1234567890" ""))))

(defn- last-word [letters]
  (as-> letters l
    (if (and (not (empty? letters)) (= " " (first letters)))
      (rest l) l)
    (str/join (reverse (take-while #(not= " " %) l)))))

(defn- recognize? [owner word]
  (let [trie (mash-words/get-word-trie)
        letters (seq word)]
    (= "" (get-in trie (conj (into [] (map str/lower-case letters)) "")))))

(defmethod mode/dispatch-keydown :freestyle
  [cursor owner key]
  (om/transact!
   (om/observe owner (mash-state/lines))
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
          (command/dispatch-enter % owner))
        ;; Ignore everything else
        :default %))))

(defmethod mode/line-render-state :freestyle
  [cursor owner state]
  (let [words (str/split (str/join (reverse (:letters cursor))) " ")
        spacing (contains? #{:typing-space :mashing-space} (first (:state cursor)))
        commanding (or
                    (contains? @command/valid-commands (first words))
                    (contains? @set-manifest/sets (first words)))
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
             (if (:focus state) (cons command-prompt r) r)))))

(defmethod mode/initial-line-state :freestyle [state]
  (merge state
         {:state [:empty]
          :trie (mash-words/get-word-trie)
          :trie-stack []
          :command-trie (command/get-command-trie)
          :command-trie-stack []
          :letters []}))

;; (defmethod command/dispatch-enter "say"
;;   [cursor]
;;   (go (>! speech/say (apply str (drop 3 (reverse (get-in cursor [:active :letters]))))))
;;   (as-> cursor c
;;     (assoc c :history (cons (:active c) (:history c)))
;;     (assoc c :active (mash-state/initial-line-state))))
;; (swap! command/valid-commands #(conj % "say"))

;; (defmethod command/dispatch-enter "wiki"
;;   [cursor]
;;   (go (>! wiki/search (apply str (drop 4 (reverse (get-in cursor [:active :letters]))))))
;;   (as-> cursor c
;;     (assoc c :history (cons (:active c) (:history c)))
;;     (assoc c :active (mash-state/initial-line-state))))
;; (swap! command/valid-commands #(conj % "wiki"))

;; (defmethod command/dispatch-enter "wolfram"
;;   [cursor]
;;   (go (>! wolfram/answer (apply str (drop 7 (reverse (get-in cursor [:active :letters]))))))
;;   (as-> cursor c
;;     (assoc c :history (cons (:active c) (:history c)))
;;      (assoc c :active (mash-state/initial-line-state))))
;; (swap! command/valid-commands #(conj % "wolfram"))
