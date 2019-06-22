(ns mash-commander.nix.filesystem
  (:require [clojure.string :as str]
            [mash-commander.state :as state]
            [mash-commander.trie :as trie]
            [mash-commander.mode :as mode]
            [om.core :as om :include-macros true]))

(defn fs-cursor []
  (om/ref-cursor (get-in (om/root-cursor state/app-state) [:characters :nix :fs])))

(defn appearance-cursor []
  (om/ref-cursor (get-in (om/root-cursor state/app-state) [:characters :nix :appearance])))

(defn commands [dir]
  (let [files (seq (:files dir))]
    (apply concat 
           ;; Flattened list of lists of command structures
           (map #(cond
                   ;;; directory
                   (= :dir (:type (second %)))
                   (commands (second %))
                   ;;; executable files
                   (and (= :file (:type (second %)))
                        (contains? (:mod (second %)) :x))
                   [(assoc (second %) :name (first %))]
                   ;;; nothing
                   :default [])
                files))))

(defn command-trie []
  (let [c (map :name (commands (:root @(fs-cursor))))]
    (trie/build c)))
    
(defn command-map []
  (reduce #(assoc %1 (:name %2) %2) {} (commands (:root @(fs-cursor)))))

(defn cwd []
  (let [fs @(fs-cursor)]
    (get-in (:root fs) (interleave (repeat :files) (:cwd fs)))))

(defn create-file-in-fs [fs name file]
  (let [path (apply concat [:root] (interleave (repeat :files) (:cwd fs)) [[:files name]])]
    (assoc-in fs path file)))

(defn ls-cmd []
  (str/join "\t" (keys (:files (cwd)))))

(defn cd-cmd [param]
  (if (= ".." param)
    (om/transact! (fs-cursor) #(assoc % :cwd (drop-last (:cwd %))))
    (let [path (str/split param "/")
          cwd (:cwd @(fs-cursor))
          new-cwd (concat cwd path)]
      (om/transact! (fs-cursor) #(assoc % :cwd new-cwd))))
  nil)

(defn clear-cmd []
  (om/transact!
   (state/lines)
   #(as-> % c
      (assoc-in c [:history] [])
      (assoc-in c [:active] (mode/initial-line-state {:mode :nix}))))
  nil)

(defn help-cmd [param]
  (let [cmd (get (command-map) param)]
    (cond
      (and cmd (:help cmd))
      (:help cmd)
      cmd
      (str (:args cmd))
      :default
      (str param " is not a command"))))        

(defn set-cmd [param]
  (let [kv (str/split param " ")]
    (when (= "color" (first kv))
      (om/transact! (appearance-cursor) #(assoc % :color (second kv))))
    nil))

(defn touch-cmd [param]
  (cond
    (contains? param (:files (cwd)))
    "500 error: file already exists"
    (not (contains? (:mod (cwd)) :w))
    "409 error: directory is not writable"
    :default
    (om/transact!
     (fs-cursor)
     #(create-file-in-fs % param {:mod #{:r :w}
                                  :type :file
                                  :contents ""}))))

(defn init []
  (om/update!
   (fs-cursor)
   {:root {:mod #{:r}
           :type :dir
           :files {"bin" {:mod #{:r}
                          :type :dir
                          :files {"ls" {:mod #{:x}
                                        :type :file
                                        :fn ls-cmd
                                        :args []
                                        :help "`ls` tells you what is in your current directory."}
                                  "cd" {:mod #{:x}
                                        :type :file
                                        :fn cd-cmd
                                        :args [:dir]
                                        :help "`cd` changes your current directory."}
                                  "clear" {:mod #{:x}
                                           :type :file
                                           :fn clear-cmd
                                           :args []
                                           :help "`clear` clears the screen."}
                                  "help" {:mod #{:x}
                                          :type :file
                                          :fn help-cmd
                                          :args [:cmd]
                                          :help "`help` teaches you about other commands."}
                                  "set" {:mod #{:x}
                                         :type :file
                                         :fn set-cmd
                                         :args [["color red" "color blue" "color green" "color white"]]
                                         :help "`set` appearance. E.g `set color red`."}
                                  "touch" {:mod #{:x}
                                          :type :file
                                          :fn touch-cmd
                                          :args [:alpha]
                                          :help "`touch` creates an empty file."}}}
                   "usr" {:mod #{:r :w}
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

;; TODO: cache delay evaluation
(defn- alpha-trie [level]
  (let [alpha "abcdefghijklmnopqrstuvwxyz"
        t (reduce #(assoc %1 %2 (delay (alpha-trie (+ 1 level)))) {} alpha)]
    (if (< 0 level) (assoc t "" "") t)))

(defn args-trie [args-spec]
  (cond
    ;; Looking for a valid filename
    (= :file args-spec)
    (let [root @(fs-cursor)
          cwd (get-in (:root root) (:cwd root))]
      (if (empty? cwd)
        (trie/build (cons ".." (files cwd)))
        (trie/build (files cwd))))
    ;; Looking for a valid directory in the current working directory
    (= :dir args-spec)
    (let [everything (seq (:files (cwd)))
          dirs (filter #(= :dir (:type (second %))) everything)
          names (map first dirs)]
      (if (empty? (:cwd @(fs-cursor)))
        (trie/build names)
        (trie/build (cons ".." names))))
    ;; Looking for a valid command
    (= :cmd args-spec)
    (trie/build (keys (command-map)))
    ;; Any characters a-z
    :alpha
    (alpha-trie 0)
    ;; Pre-specified parameters
    :default
    (trie/build args-spec)))
