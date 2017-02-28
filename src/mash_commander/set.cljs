(ns mash-commander.set
  (:require [mash-commander.trie :as trie]
            [mash-commander.speech :as speech]))

(defn- load-words [set]
  (let [words (map #(get-in % ["when" "type"]) set)]
    (map (fn [word]
           (cond
             (empty? word) {:error "Word must not be empty."}
             (not (re-matches #"^[a-z]+( [a-z]+)*$" word))
             {:error (str "Word must contain only lower case letters, "
                          "single spaces and no leading or trailing "
                          "whitespace")}
             :default word)))))

(defn load [set]
  (let [words (load-words set)
        word-trie (trie/build words)]
    {:set set :trie word-trie}))
