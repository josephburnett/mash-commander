(ns mash-commander.nix.filesystem
  (:require [clojure.string :as str]
            [mash-commander.state :as state]
            [mash-commander.trie :as trie]
            [om.core :as om :include-macros true]))

(defn root-cursor []
  (om/ref-cursor (get-in (om/root-cursor state/app-state) [:characters :nix :root])))

(declare files)

(defn ls []
  (let [root @(root-cursor)
        dir (get-in (:fs root) (interleave (repeat :files) (:cwd root)))]
    (str/join "\t" (keys (:files dir)))))

(defn cd [dir]
  (if (= ["." "."] dir)
    (om/transact! (root-cursor) #(assoc % :cwd (drop-last (:cwd %))))
    (let [path (str/split (str/join "" dir) "/")
          cwd (:cwd @(root-cursor))
          new-cwd (concat cwd path)]
      (om/transact! (root-cursor) #(assoc % :cwd new-cwd))))
  nil)

(defn init []
  (om/update!
   (root-cursor)
   {:fs {:mod #{:r}
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
                                      :args [:file]}}}
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
  (let [c (map :name (commands (:fs @(root-cursor))))]
    (trie/build c)))
    
(defn command-map []
  (reduce #(assoc %1 (:name %2) %2) {} (commands (:fs @(root-cursor)))))

(defn args-trie [args-spec]
  (cond
    ;; Looking for a valid filename
    (= :file args-spec)
    (let [root @(root-cursor)
          cwd (get-in (:fs root) (:cwd root))]
      (if (empty? cwd)
        (trie/build (cons ".." (files cwd)))
        (trie/build (files cwd))))
    ;; Pre-specified parameters
    :default
    (trie/build args-spec)))
