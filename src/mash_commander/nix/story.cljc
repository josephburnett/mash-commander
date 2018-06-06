(ns mash-commander.nix.story)

(def pages
  {;; Learning `ls` and typing commands.
   :page-1 {:say "Hi, I'm Nix."
            :then {:say "Type `ls` and then press Enter to look around!"
                   :wait-event {:type :new-line :line "ls"}
                   :then {:goto :page-2}}}
   :page-2 {:say "Good job! The results of your command are shown underneath in the grey box."
            :goto :page-3}
   ;; Learning `cd` and the command prompt.
   :page-3 {:say "Let's try something new. You can change directories with the `cd` command."
            :then {:say "Try changing into the `bin` directory by typing `cd bin`."
                   :wait-event {:type :new-line :line "cd bin"}
                   :then {:goto :page-4}}}
   :page-4 {:say "Nice! Now your current directory is `bin`."
            :then {:say "Notice the green nix: command prompt changed to tell you where you are."
                   :goto :page-5}}
   ;; Learning `clear` and discovering new commands.
   :page-5 {:say "Now use `ls` again to list the files in the `bin` directory."
            :wait-event {:type :new-line :line "ls"}
            :then {:say "Hey look! The `ls` and `cd` commands are actually just files in `bin`!"
                   :then {:say "Do you see the `clear` command? What do you think it does?"
                          :wait-event {:type :new-line :line "clear"}
                          :then {:goto :page-6}}}}
   :page-6 {:say "Cool. That's all for now. I'm still a work in progress!"}})
                   
                   
