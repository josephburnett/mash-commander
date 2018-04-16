(ns mash-commander.nix.nix-mode
  (:require [mash-commander.freestyle.command :as command]
            [mash-commander.mode :as mode]
            [mash-commander.nix.filesystem :as fs]
            [mash-commander.nix.command :as nix-command]
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
  (print key))

(defmethod mode/line-render-state :nix
  [line owner state]
  [(dom/span nil "nix line")])

(defmethod mode/initial-line-state :nix
  [state]
  (merge state
         {:command-trie (nix-command/command-trie)
          :command-trie-stack []
          :letters []}))

(defmethod mode/key-potential :nix
  [line key]
  (cond
    ;; Command potential
    (contains? (:command-trie line) key) :command
    ;; All other keys disabled
    :default :disabled))
