(ns mash-commander.trie
  (:require [clojure.string :as str]))

(defn- add-word [trie word]
  (let [letters (into [] (map str/lower-case (str/split word #"")))]
    (if (or (empty? letters) (= "#" (first letters)))
      trie (assoc-in trie (conj letters "") ""))))

(defn build [words]
  (reduce add-word {} words))

(defn trie-merge [a b]
  (merge-with trie-merge a b))
