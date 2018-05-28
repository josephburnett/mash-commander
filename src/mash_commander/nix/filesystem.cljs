(ns mash-commander.nix.filesystem
  (:require [clojure.string :as str]))

(def root (atom {}))
(declare files)

(defn ls []
  (let [dir (get-in (:fs @root) (:cwd @root))]
    (str/join "\t" (keys (:files dir)))))

(defn cd [dir]
  (let [path (str/split (str/join "" dir) "/")
        cwd (:cwd @root)
        new-cwd (concat cwd path)]
    (swap! root #(assoc % :cwd new-cwd))))

(reset! root
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
       :ps []})

(defn init []
  (print "nix up"))

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
   
