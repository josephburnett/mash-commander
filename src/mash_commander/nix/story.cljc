(ns mash-commander.nix.story)

(def pages
  {:page-1 {:say "Hi, I'm Nix."
            :then {:say "Type `ls` to look around!"
                   :wait-event {:type :new-line :line "ls"}
                   :then {:goto :page-2}}}
   :page-2 {:say "Good job! What you are seeing is the files in your current directory."}})
