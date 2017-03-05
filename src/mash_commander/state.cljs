(ns mash-commander.state
  (:require [om.core :as om :include-macros true]))

(defn initial-line-state []
  {:mode :freestyle
   :state [:empty]
   :letters []})

(defonce app-state
  (atom {:lines {:active (initial-line-state)
                 :history []}
         :words {}
         :sets {}}))

(defn lines []
  (om/ref-cursor (:lines (om/root-cursor app-state))))

(defn words []
  (om/ref-cursor (:words (om/root-cursor app-state))))

(defn sets []
  (om/ref-cursor (:sets (om/root-cursor app-state))))

(defn initial-line-state-set [owner set-name]
  (let [trie (get-in (om/observe owner (sets)) [set-name :trie])]
    {:mode :set
     :set set-name
     :trie trie
     :trie-stack []
     :letters []}))

