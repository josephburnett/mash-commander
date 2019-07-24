(ns mash-commander.nix.story
  (:require [clojure.string :as string]))

(defn- if-line [want then]
  (fn [got _] (when (= want (:line got)) then)))

(defn- if-inotify-path [want then]
  (fn [got _] (when (= want (reverse (:path got))) then)))

(def pages
  {;; Debugging
   :debug
   {:when-events [{:type :new-line
                   :then (if-line "ls" {:say "saw `ls` command"})
                   :recur true}
                  {:type :inotify
                   :then (if-inotify-path ["usr" "key"] {:say "key touched"})}]}
   :start
   {:allow []
    :when-events [;; Delete a bad file from the usr directory.
                  {:type :inotify
                   :then #(let [file %]
                           (when (and (= ["bad" "usr"] (:path %))
                                      (= "11001010101010101" (get-in % [:file :contents]))))
                           {:goto :bad-file-gone})}]
    :then {:goto :help-me}}
   
   :help-me
   {:allow ["cd usr"]
    :when-events [;; Enter usr directory.
                  {:type :new-line
                   :then (if-line "cd usr" {:say "Yeah, it's right there."
                                            :allow ["ls"]
                                            :then {:say "Type `ls` to see it."}})}
                  {:type :new-line
                   :then (if-line "ls" {:goto :see-the-bad-file})}]
    :say "Hello. I seem to have gotten a bad file stuck in my usr directory."
    :then {:say "Can you help me remove it?"
           :then {:say "Type `cd usr`."}}}

   :see-the-bad-file
   {:allow ["ls" "rm bad" "cat bad"]
    :then {:say "Yeah, there it is."
           :then {:say "It should have 11001010101010101 inside."
                  :then {:say "Type `rm bad` to remove it."}}}}
   
   :bad-file-gone
   {:say "Wow .. I feel so much better!"
    :then {:say "Thank you."}}})
                   
