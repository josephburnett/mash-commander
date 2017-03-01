(ns mash-commander.state
  (:require [om.core :as om :include-macros true]))

(defn initial-line-state []
  {:mode :freestyle
   :state [:empty]
   :letters []})

(defn initial-line-state-set []
  {:mode :set
   :set "animals"
   :trie {"d" {"o" {"g" {"" ""}}}
          "c" {"o" {"w" {"" ""}}
               "a" {"t" {"" ""}}}}
   :trie-stack []
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
