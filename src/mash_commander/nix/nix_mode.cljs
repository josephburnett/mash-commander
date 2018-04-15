(ns mash-commander.nix.nix-mode
  (:require [mash-commander.freestyle.command :as command]
            [mash-commander.mode :as mode]
            [om.dom :as dom :include-macros true]))

;; Enter nix mode
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
  (dom/div nil "nix line"))

(defmethod mode/initial-line-state :nix
  [state]
  state)

(defmethod mode/key-potential :nix
  [line key]
  :disabled)
