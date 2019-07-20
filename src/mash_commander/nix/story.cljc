(ns mash-commander.nix.story
  (:require [clojure.string :as string]))

(defn- if-line [want then]
  (fn [got _] (when (= want (:line got)) then)))

(defn- if-inotify-path [want then]
  (fn [got _] (when (= want (reverse (:path got))) then)))

(def pages
  {;; Debugging
   :page-0 {:when-events [{:type :new-line
                           :then (if-line "ls" {:say "saw `ls` command"})
                           :recur true}
                          {:type :inotify
                           :then (if-inotify-path ["usr" "key"] {:say "key touched"})}]}
   ;; Learning `ls` and typing commands.
   :page-1 {:allow []
            :say "Hi, I'm Nix."
            :then {:say "Type `ls` and then press Enter to look around!"
                   :then {
                          :allow ["ls"]
                          :wait-event {:type :new-line
                                       :then (if-line "ls" {:goto :page-2})}}}}
   :page-2 {:allow []
            :say "Good job! The results of your command are shown underneath in the grey box."
            :goto :page-3}
   ;; Learning `cd` and the command prompt.
   :page-3 {:allow []
            :say "Let's try something new. You can change directories with the `cd` command."
            :then {:say "Try changing into the `bin` directory by typing `cd bin`."
                   :then {:allow ["cd bin"]
                          :wait-event {:type :new-line
                                       :then (if-line "cd bin" {:goto :page-4})}}}}
   :page-4 {:allow []
            :say "Nice! Now your current directory is `bin`."
            :then {:say "Notice the green nix: command prompt changed to tell you where you are."
                   :goto :page-5}}
   ;; Learning `clear` and discovering new commands.
   :page-5 {:allow []
            :say "Now use `ls` again to list the files in the `bin` directory."
            :then {:allow ["ls"]
                   :wait-event {:type :new-line
                                :then (if-line "ls" {})}
                   :then {:allow []
                          :say "Hey look! The `ls` and `cd` commands are actually just files in `bin`!"
                          :then {:allow ["clear"]
                                 :say "Do you see the `clear` command? What do you think it does?"
                                 :wait-event {:type :new-line
                                              :then (if-line "clear" {})}
                                 :then {:goto :page-6}}}}}
   :page-6 {:say "Cool. That's all for now. I'm still a work in progress!"
            :wait-event {:type :key-down
                         :then (fn [_ _] true)}
            :goto :page-7}
   :page-7 {:say "Bonus."}})
                   
                   
