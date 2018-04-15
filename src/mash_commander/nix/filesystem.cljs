(ns mash-commander.nix.filesystem)

(defonce root
  (atom {:chmod #{:r}
         :ls {"bin" {:chmod #{:r}
                     :ls {"ls" {:chmod #{:r}
                                :file "joe was here"}}}}}))

(defn init []
  ;; TODO: Load initial filesystem state
  (print "nix up"))
