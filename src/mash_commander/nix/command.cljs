(ns mash-commander.nix.command
  (:require [mash-commander.nix.filesystem :as fs]
            [mash-commander.trie :as trie]))

(declare command-line)

(defn- command-list [dir]
  (let [files (seq (:files dir))]
    (apply concat 
           ;; Flattened list of lists of commands
           (map #(cond
                   (= :dir (:type (second %))) (command-list (second %))
                   (and (= :file (:type (second %))) (contains? (:mod (second %)) :x)) [(first %)]
                   :default [])
                files))))

(defn command-trie []
  (let [commands (command-list (:fs @fs/root))]
    (trie/build commands)))
    
