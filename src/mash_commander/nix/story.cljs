(ns mash-commander.nix.story
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [mash-commander.speech :as speech]
            [cljs.core.async :refer [pub sub chan put! close! <! >!]]
            [om.core :as om :include-macros true]))

(def story-line
  {:page-1 {:say "Hi, I'm Nix."
            :then {:say "Type `ls` to look around!"
                   :wait #(and (= :new-line (:type %))
                               (= "ls" (:line %)))
                   :then {:goto :page-2}}}
   :page-2 {:say "Good job! What are seeing are the folders in the root directory."}})

(def event-chan (chan))
(def event-pub (pub event-chan :type))

(defn wait [test]
  (let [done (chan)
        event-sub (chan)]
    (sub event-pub :new-line event-sub)
    (go-loop []
      (let [event (<! event-sub)]
        (if (test event)
          (do
            (close! event-sub)
            (close! done))
          (recur)))
      done)))
    
(defn run-page [cursor page]
  (let [done (chan)]
    (go
      (when (:say page)
        (<! (speech/wait-say (:say page))))
      (when (:wait page)
        (<! (wait (:wait page))))
      (when (:then page)
        (<! (run-page cursor (:then page))))
      (when (:goto page)
        (let [new-page (:goto page)]
          (om/transact! cursor #(assoc % :current-page (:goto page))))
        (close! done))
    done)))

(defn begin [cursor]
  (go-loop []
    (let [page (get story-line (:current-page cursor))]
      (<! (run-page cursor page)))
    (recur)))
          
