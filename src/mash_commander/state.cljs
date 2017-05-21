(ns mash-commander.state
  (:require [mash-commander.mode :as mode]
            [om.core :as om :include-macros true]))

(defonce app-state
  (atom {:lines {:history []}
         :words {}}))

(defn lines []
  (om/ref-cursor (:lines (om/root-cursor app-state))))

(defn words []
  (om/ref-cursor (:words (om/root-cursor app-state))))

(defn load []
  (print "State loaded."))

(defn init []
  (swap! app-state #(assoc-in % [:lines :active]
                              (mode/initial-line-state {:mode :freestyle}))))
    
