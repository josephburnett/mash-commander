(ns mash-commander.nix.filesystem)

(declare ls)

(defonce root
  (atom {:fs {:mod #{:r}
              :type :dir
              :files {"bin" {:mod #{:r}
                             :type :dir
                             :files {"ls" {:mod #{:x}
                                           :type :file
                                           :fn ls
                                           :args []}}}}}
         :cwd []
         :ps []}))

(defn ls []
  (let [dir (get-in (:fs @root) (:cwd @root))]
    (keys (:ls dir))))

(defn init []
  (print "nix up"))
