(ns mash-commander.nix.nix-mode
  (:require [clojure.string :as str]
            [mash-commander.freestyle.command :as command]
            [mash-commander.mode :as mode]
            [mash-commander.nix.filesystem :as fs]
            [mash-commander.nix.command :as nix-command]
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
        ;; Complete command
        (and (= " " key)
             (= :command mode)
             (contains? (:command-trie (:active %)) ""))
        (as-> % c
          (assoc-in c [:active :command-trie-stack] (cons trie stack))
          (assoc-in c [:active :command-trie] (trie/build (first (get-in (nix-command/command-map) [(str/join "" (reverse letters)) :args]))))
          (assoc-in c [:active :letters] (cons key letters))
          (assoc-in c [:active :nix-mode-stack] (cons mode mode-stack))
          (assoc-in c [:active :nix-mode] :args))
        ;; Run command
        (and (= "Enter" key)
             (contains? (:command-trie (:active %)) ""))
        (as-> % c
          (let [command-string (str/join "" (take-while (fn [l] (not (= " " l))) (reverse letters)))
                func (get-in (nix-command/command-map) [command-string :fn])
                result (func (reverse letters))]
            (assoc-in c [:active :result] result))
          (assoc-in c [:history] (cons (:active c) (:history c)))
          (assoc-in c [:active] (mode/initial-line-state {:mode :nix})))
        ;; Ignore everything else
        :default %))))


(defmethod mode/line-render-state :nix
  [line owner state]
  (let [command (dom/span #js {:style #js {:color "#33f"}}
                          (first (str/split (str/join (reverse (:letters line))) " ")))
        args (str/join "" (drop-while #(not (= " " %)) (reverse (:letters line))))
        prompt (dom/span #js {:style #js {:color "#080"
                                          :fontWeight "bold"}}
                         (str/join "/" (concat ["nix:"] (:cwd line) ["$ "])))
        cursor (dom/span #js {:style #js {:color "#900"}} "\u2588")
        result (dom/span #js {:style #js {:color "#fff"
                                          :fontSize "0.7em"
                                          :margin "1em 0.5em"
                                          :padding "0.5em"
                                          :backgroundColor "#222"
                                          :border "1px solid #555"}} (:result line))
        arrow (dom/span #js {:style #js {:color "#33f"}} "\u21B3")]
    (if (:focus state)
      [prompt command args cursor]
      [(dom/div nil command args) (dom/div nil arrow result)])))

(defmethod mode/initial-line-state :nix
  [state]
  (merge state
         {:command-trie (nix-command/command-trie)
          :command-trie-stack []
          :command-map (nix-command/command-map)
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
         (= :command (:nix-mode line))
         (contains? (:command-trie line) "")) :command
    ;; Arguments allowed
    (and (= " " key)
         (= :command (:nix-mode line))
         (contains? (:command-trie line) "")) :typing
    ;; Argument potential
    (and (= :args (:nix-mode line))
         (contains? (:command-trie line) key)) :typing
    ;; All other keys disabled
    :default :disabled))
