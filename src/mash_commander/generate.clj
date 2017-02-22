(ns mash-commander.generate
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn add-word [trie word]
  (let [letters (into [] (map str/lower-case (str/split word #"")))]
    (if (or (empty? letters) (= "#" (first letters)))
      trie (assoc-in trie (conj letters "") ""))))

(defn -main [_ root]
  (println "Generating word trie in project root:" root)
  (with-open [rdr (io/reader (str root "/words.txt"))]
    (let [trie (reduce add-word {} (line-seq rdr))]
      (with-open [wrtr (io/writer (str root "/resources/public/words.json"))]
        (.write wrtr (json/write-str trie)))))
  (println "Done."))
  
