(ns mash-commander.mode
  (:require [om.dom :as dom :include-macros true]))

(defmulti dispatch-keydown
  (fn [line owner e]
    (get line :mode)))

(defmulti line-render-state
  (fn [line owner state]
    (get line :mode)))

(defmulti initial-line-state
  (fn [line]
    (get line :mode)))

(defmulti key-potential
  (fn [line key]
    (get line :mode)))
