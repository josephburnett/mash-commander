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
(defmethod mode/dispatch-keydown :nix
  [line owner key]
  (om/transact!
   (om/observe owner (mash-state/lines))
   #(let [trie (:command-trie (:active %))
          stack (:command-trie-stack (:active %))
          mode (:nix-mode (:active %))
          mode-stack (:nix-mode-stack (:active %))
          args (:nix-args (:active %))
          args-stack (:nix-args-stack (:active %))
          letters (:letters (:active %))]
      (cond
        ;; Backspace
        (and (= "Backspace" key) (not (empty? letters)))
        (as-> % c
          (assoc-in c [:active :letters] (rest letters))
          (assoc-in c [:active :nix-mode] (first mode-stack))
          (assoc-in c [:active :nix-mode-stack] (rest mode-stack))
          (assoc-in c [:active :nix-args] (first args-stack))
          (assoc-in c [:active :nix-args-stack] (rest args-stack))
          (assoc-in c [:active :command-trie] (first stack))
          (assoc-in c [:active :command-trie-stack] (rest stack)))
        ;; Valid transition
        (contains? (:command-trie (:active %)) key)
        (as-> % c
          (assoc-in c [:active :command-trie-stack] (cons trie stack))
          (assoc-in c [:active :command-trie] (get trie key))
          (assoc-in c [:active :nix-mode-stack] (cons mode mode-stack))
          (assoc-in c [:active :nix-args-stack] (cons args args-stack))
          (assoc-in c [:active :letters] (cons key letters)))
        ;; Command with args
        (and (= " " key)
             (= :command mode)
             (contains? (:command-trie (:active %)) "")
             (not (empty? (get-in (fs/command-map) [(str/join "" (reverse letters)) :args]))))
        (as-> % c
          (assoc-in c [:active :command-trie-stack] (cons trie stack))
          (assoc-in c [:active :command-trie] (fs/args-trie (first (get-in (fs/command-map) [(str/join "" (reverse letters)) :args]))))
          (assoc-in c [:active :letters] (cons key letters))
          (assoc-in c [:active :nix-mode-stack] (cons mode mode-stack))
          (assoc-in c [:active :nix-mode] :args))
        ;; Run command
        (and (= "Enter" key)
             (or (and (= :command mode)
                      (= [] (get-in (fs/command-map) [(str/join "" (reverse letters)) :args])))
                 (= :args mode))
             (contains? (:command-trie (:active %)) ""))
        (do
          (let [last-line (str/join "" (reverse letters))
                new-c (as-> % c
                        (let [command-string (str/join "" (take-while (fn [l] (not (= " " l))) (reverse letters)))
                              func (get-in (fs/command-map) [command-string :fn])
                              result (func (drop (+ 1 (count command-string)) (reverse letters)))]
                          (assoc-in c [:active :result] result))
                        (assoc-in c [:history] (cons (:active c) (:history c)))
                        (assoc-in c [:active] (mode/initial-line-state {:mode :nix}))
                        (assoc-in c [:characters :nix :last-line] last-line))]
            (put! character/event-chan {:type :new-line :line last-line})
            new-c))
        ;; Ignore everything else
        :default %))))

(defmethod mode/line-render-state :nix
  [line owner state]
  (let [command (dom/span #js {:style #js {:color "#33f"}}
                          (first (str/split (str/join (reverse (:letters line))) " ")))
        args (str/join "" (drop-while #(not (= " " %)) (reverse (:letters line))))
        prompt (dom/span #js {:style #js {:color "#080"
                                          :fontWeight "bold"}}
                         (str/join "/" (concat ["nix:"] (:cwd @fs/root) ["$ "])))
        cursor (dom/span #js {:style #js {:color "#900"}} "\u2588")
        result (dom/span #js {:style #js {:color "#fff"
                                          :fontSize "0.7em"
                                          :margin "1em 0.5em"
                                          :padding "0.5em"
                                          :backgroundColor "#222"
                                          :border "1px solid #555"}} (:result line))
        arrow (when (:letters line) (dom/span #js {:style #js {:color "#33f"}} "\u21B3"))]
    (if (:focus state)
      [(dom/div #js {:style #js {:padding "0 0 0.7em 0"}} prompt command args cursor)]
      [(dom/div nil command args)
       (when (:result line)
         (dom/div #js {:style #js {:padding "0 0 0.7em 0"}} arrow result))])))

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
