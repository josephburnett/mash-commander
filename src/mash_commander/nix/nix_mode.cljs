(ns mash-commander.nix.nix-mode
  (:require [clojure.string :as str]
            [cljs.core.async :refer [put!]]
            [mash-commander.freestyle.command :as command]
            [mash-commander.mode :as mode]
            [mash-commander.nix.filesystem :as fs]
            [mash-commander.nix.character :as character]
            [mash-commander.state :as mash-state]
            [mash-commander.trie :as trie]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;; Register `nix` as a freestyle command
(defmethod command/dispatch-enter "nix"
  [cursor _]
  (as-> cursor c
    (assoc c :history [])
    (assoc c :active (mode/initial-line-state {:mode :nix}))))
(command/add-command "nix")

;; Mode methods

(defmethod mode/can-restore :nix [_ _ _] false)

(defn- backspace [owner]
  (om/transact!
   (om/observe owner (mash-state/lines))
   #(as-> % c
      (assoc-in c [:active :letters] (rest (get-in c [:active :letters])))
      (assoc-in c [:active :nix-mode] (first (get-in c [:active :nix-mode-stack])))
      (assoc-in c [:active :nix-mode-stack] (rest (get-in c [:active :nix-mode-stack])))
      (assoc-in c [:active :nix-args] (first (get-in c [:active :nix-args-stack])))
      (assoc-in c [:active :nix-args-stack] (rest (get-in c [:active :nix-argx-stack])))
      (assoc-in c [:active :command-trie] (first (get-in c [:active :command-trie-stack])))
      (assoc-in c [:active :command-trie-stack] (rest (get-in c [:active :command-trie-stack]))))))

(defn- transition [owner key]
  (om/transact!
   (om/observe owner (mash-state/lines))
   #(as-> % c
      (assoc-in c [:active :command-trie-stack] (cons (get-in c [:active :command-trie])
                                                      (get-in c [:active :command-trie-stack])))
      (assoc-in c [:active :command-trie] (get-in c [:active :command-trie key]))
      (assoc-in c [:active :nix-mode-stack] (cons (get-in c [:active :nix-mode])
                                                  (get-in c [:active :nix-mode-stack])))
      (assoc-in c [:active :nix-args-stack] (cons (get-in c [:active :nix-args])
                                                  (get-in c [:active :nix-args-stack])))
      (assoc-in c [:active :letters] (cons key (get-in c [:active :letters]))))))

(defn- command-args [owner key]
  (om/transact!
   (om/observe owner (mash-state/lines))
   #(as-> % c
      (assoc-in c [:active :command-trie-stack] (cons (get-in c [:active :command-trie])
                                                      (get-in c [:active :command-trie-stack])))
      (let [args (first (get-in (fs/command-map) [(str/join "" (reverse (get-in c [:active :letters]))) :args]))]
        (assoc-in c [:active :command-trie] (fs/args-trie args)))
      (assoc-in c [:active :letters] (cons key (get-in c [:active :letters])))
      (assoc-in c [:active :nix-mode-stack] (cons (get-in c [:active :nix-mode])
                                                  (get-in c [:active :nix-mode-stack])))
      (assoc-in c [:active :nix-mode] :args))))

(defn- run-command [owner]
  (let [letters (get-in @(mash-state/lines) [:active :letters])
        last-line (str/join "" (reverse letters))
        command-string (str/join "" (take-while #(not (= " " %)) (reverse letters)))
        func (get-in (fs/command-map) [command-string :fn])
        params (str/join (drop (+ 1 (count command-string)) (reverse letters)))
        result (func params)]
    (om/transact!
     (om/observe owner (mash-state/lines))
     #(as-> % c
        (assoc-in c [:active :result] result)
        (assoc-in c [:history] (cons (:active c) (:history c)))
        (assoc-in c [:active] (mode/initial-line-state {:mode :nix}))))
    (put! character/event-chan {:type :new-line :line last-line})))

(defmethod mode/dispatch-keydown :nix
  [line owner key]
  (let [active (get-in @(mash-state/lines) [:active])]
    (put! character/event-chan {:type :key-down :key key})
    (cond
      ;; Backspace
      (and (= "Backspace" key) (not (empty? (:letters active))))
      (backspace owner)
      ;; Valid transition
      (contains? (:command-trie active) key)
      (transition owner key)
      ;; Command with args
      (and (= " " key)
           (= :command (:nix-mode active))
           (contains? (:command-trie active) "")
           (not (empty? (get-in (fs/command-map) [(str/join "" (reverse (:letters active))) :args]))))
      (command-args owner key)
      ;; Run command
      (and (= "Enter" key)
           (or (and (= :command (:nix-mode active))
                    (= [] (get-in (fs/command-map) [(str/join "" (reverse (:letters active))) :args])))
               (= :args (:nix-mode active)))
           (contains? (:command-trie active) ""))
      (run-command owner)
      ;; Ignore everything else
      :default nil)))

(defmethod mode/line-render-state :nix
  [line owner state]
  (let [command (dom/span #js {:style #js {:color "#33f"}}
                          (first (str/split (str/join (reverse (:letters line))) " ")))
        args (str/join "" (drop-while #(not (= " " %)) (reverse (:letters line))))
        prompt (dom/span #js {:style #js {:color "#080"
                                          :fontWeight "bold"}}
                         (str/join "/" (concat ["nix:"] (:cwd @(fs/fs-cursor)) ["$ "])))
        cursor (dom/span #js {:style #js {:color "#900"}} "\u2588")
        result (dom/span #js {:style #js {:color "#fff"
                                          :fontSize "0.7em"
                                          :margin "1em 0.5em"
                                          :padding "0.5em"
                                          :backgroundColor "#222"
                                          :border "1px solid #555"}} (:result line))
        arrow (when (:letters line) (dom/span #js {:style #js {:color "#33f"}} "\u21B3"))]
    (if (:focus state)
      [(dom/div #js {:style #js {:padding "0 0 0.4em 0"}} prompt command args cursor)]
      [(dom/div nil command args)
       (when (:result line)
         (dom/div #js {:style #js {:padding "0 0 0.4em 0"}} arrow result))])))

(defmethod mode/initial-line-state :nix
  [state]
  (merge state
         {:command-trie (fs/command-trie)
          :command-trie-stack []
          :command-map (fs/command-map)
          :nix-mode :command
          :nix-mode-stack []
          :letters []}))

(defmethod mode/key-potential :nix
  [line key]
  (cond
    ;; Command potential
    (and (= :command (:nix-mode line))
         (contains? (:command-trie line) key)) :command
    ;; Complete command
    (and (= "Enter" key)
         (or (and (= :command (:nix-mode line))
                  (= [] (get-in (fs/command-map) [(str/join "" (reverse (:letters line))) :args])))
             (= :args (:nix-mode line)))
         (contains? (:command-trie line) "")) :command
    ;; Arguments allowed
    (and (= " " key)
         (= :command (:nix-mode line))
         (contains? (:command-trie line) "")
         (not (empty? (get-in (fs/command-map) [(str/join "" (reverse (:letters line))) :args])))) :typing
    ;; Argument potential
    (and (= :args (:nix-mode line))
         (contains? (:command-trie line) key)) :typing
    ;; All other keys disabled
    :default :disabled))
