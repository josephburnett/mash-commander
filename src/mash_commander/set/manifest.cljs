(ns mash-commander.set.manifest
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [mash-commander.trie :as trie]
            [ajax.core :refer [GET]]
            [cljs.core.async :as async :refer [chan put! close! <! >!]]))

(def ^:private sets (atom {}))
(def ^:private set-trie (atom {}))

(defn get-set-trie []
  @set-trie)

(defn- load-words [set]
  (let [words (map #(get-in % ["when" "type"]) set)]
    (map (fn [word]
           (cond
             (empty? word) {:error "Word must not be empty."}
             (not (re-matches #"^[a-z'\-]+( [a-z'\-]+)*$" word))
             {:error (str "Word must contain only lower case letters, "
                          "single spaces and no leading or trailing "
                          "whitespace")}
             :default word))
         words)))

(defn- load-actions [set]
  (reduce
   (fn [actions a]
     (let [type (get-in a ["when" "type"])
           then (get-in a ["when" "then"])]
       (if (or (nil? type) (nil? then)) actions
           (assoc actions type then))))
   {} set))

(defn load [set]
  (let [words (load-words set)
        word-trie (trie/build words)
        actions (load-actions set)]
    {:actions actions :trie word-trie}))

(defn- load-set [name]
  (let [done (chan)]
    (go (GET (str "/sets/" name "/" name ".json")
             {:params {:response-format :json
                       :keywords? false}
              :handler (fn [s]
                         (let [loaded-set (load s)]
                           (swap! sets assoc name loaded-set)
                           (swap! set-trie #(trie/trie-merge % (trie/build (keys @sets))))
                           (close! done)))}))
    done))

(defn init []
  (let [done (chan)]
    (go (GET (str "/cache/set:manifest.json")
             {:params {:response-format :json
                       :keywords? false}
              :handler (fn [m]
                         (go
                           (<! (async/merge (map load-set m)))
                           (close! done)))}))
    done))

