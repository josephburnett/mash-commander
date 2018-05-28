(ns mash-commander.nix.filesystem
  (:require [clojure.string :as str]))

(declare root)

(defn ls []
  (let [dir (get-in (:fs @root) (:cwd @root))]
    (str/join "\t" (keys (:files dir)))))

(defonce root
  (atom {:fs {:mod #{:r}
              :type :dir
              :files {"bin" {:mod #{:r}
                             :type :dir
                             :files {"ls" {:mod #{:x}
                                           :type :file
                                           :fn ls
                                           :args [["foo" "bar"]]}}}
                      "usr" {:mod #{:r}
                             :type :dir
                             :files {}}}}
         :cwd []
         :ps []}))
(defn init []
  (print "nix up"))
