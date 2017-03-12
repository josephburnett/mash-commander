(ns mash-commander.state
  (:require [mash-commander.set.manifest :as set-manifest]
            [om.core :as om :include-macros true]))

(defn initial-line-state []
  {:mode :freestyle
   :state [:empty]
   :letters []})

(defn initial-line-state-set [name]
  (let [trie (get-in @set-manifest/sets [name :trie])]
    {:mode :set
     :set name
     :trie trie
     :trie-stack []
     :letters []}))

(defonce app-state
  (atom {:lines {:active (initial-line-state)
                 :history []}
         :words {}}))

(defn lines []
  (om/ref-cursor (:lines (om/root-cursor app-state))))

(defn words []
  (om/ref-cursor (:words (om/root-cursor app-state))))

