(ns mash-commander.nix.command
  (:require [mash-commander.nix.filesystem :as fs]
            [mash-commander.trie :as trie]))

(declare command-line)

(defn commands [dir]
  (let [files (seq (:files dir))]
    (apply concat 
           ;; Flattened list of lists of command structures
           (map #(cond
                   (= :dir (:type (second %))) (commands (second %))
                   (and (= :file (:type (second %))) (contains? (:mod (second %)) :x)) [(assoc (second %) :name (first %))]
                   :default [])
                files))))

(defn command-trie []
  (let [c (map :name (commands (:fs @fs/root)))]
    (trie/build c)))
    
(defn command-map []
  (reduce #(assoc %1 (:name %2) %2) {} (commands (:fs @fs/root))))
