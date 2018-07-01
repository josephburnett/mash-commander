(ns mash-commander.nix.filesystem
  (:require [clojure.string :as str]
            [mash-commander.state :as state]
            [mash-commander.trie :as trie]
            [mash-commander.mode :as mode]
            [om.core :as om :include-macros true]))

(defn fs-cursor []
  (om/ref-cursor (get-in (om/root-cursor state/app-state) [:characters :nix :fs])))

(declare files)

(defn ls []
  (let [fs @(fs-cursor)
        dir (get-in (:root fs) (interleave (repeat :files) (:cwd fs)))]
    (str/join "\t" (keys (:files dir)))))

(defn cd [dir]
  (if (= [".."] dir)
    (om/transact! (fs-cursor) #(assoc % :cwd (drop-last (:cwd %))))
    (let [path (str/split dir "/")
          cwd (:cwd @(fs-cursor))
          new-cwd (concat cwd path)]
      (om/transact! (fs-cursor) #(assoc % :cwd new-cwd))))
  nil)

(defn clear []
  (om/transact!
   (state/lines)
   #(as-> % c
      (assoc-in c [:history] [])
      (assoc-in c [:active] (mode/initial-line-state {:mode :nix}))))
  nil)

(defn init []
  (om/update!
   (fs-cursor)
   {:root {:mod #{:r}
           :type :dir
           :files {"bin" {:mod #{:r}
                          :type :dir
                          :files {"ls" {:mod #{:x}
                                        :type :file
                                        :fn ls
                                        :args []}
                                  "cd" {:mod #{:x}
                                        :type :file
                                        :fn cd
                                        :args [:file]}
                                  "clear" {:mod #{:x}
                                           :type :file
                                           :fn clear
                                           :args []}}}
                   "usr" {:mod #{:r}
                          :type :dir
                          :files {}}}}
    :cwd []
    :ps []}))

(defn files
  ([cwd] (files [] cwd))
  ([path cwd]
   (apply concat
          (map #(let [new-path (concat path [(first %)])]
                  (cond 
                    ;; Recursive case
                    (= :dir (:type (second %)))
                    (cons (str/join "/" new-path)
                          (files new-path (second %)))
                    ;; Base case
                    :default
                    [(str/join "/" new-path)]))
               (seq (:files cwd))))))
   
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
  (let [c (map :name (commands (:root @(fs-cursor))))]
    (trie/build c)))
    
(defn command-map []
  (reduce #(assoc %1 (:name %2) %2) {} (commands (:root @(fs-cursor)))))

(defn args-trie [args-spec]
  (cond
    ;; Looking for a valid filename
    (= :file args-spec)
    (let [root @(fs-cursor)
          cwd (get-in (:root root) (:cwd root))]
      (if (empty? cwd)
        (trie/build (cons ".." (files cwd)))
        (trie/build (files cwd))))
    ;; Pre-specified parameters
    :default
    (trie/build args-spec)))
