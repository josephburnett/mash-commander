(ns mash-commander.mode
  (:require [om.dom :as dom :include-macros true]))

(defmulti dispatch-keydown
  (fn [cursor owner e]
    (get-in cursor [:mode])))

(defmulti line-render-state
  (fn [cursor owner state]
    (get-in cursor [:mode])))

(defmethod line-render-state :default [cursor owner state]
  (dom/div nil "not ready yet"))

(defmulti initial-line-state
  (fn [line]
    (get-in line [:mode])))
