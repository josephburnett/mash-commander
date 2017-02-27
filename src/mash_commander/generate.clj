(ns mash-commander.generate
  (:require [mash-commander.trie :as trie]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(defn -main [_ root]
  (println "Generating word trie in project root:" root)
  (with-open [rdr (io/reader (str root "/words.txt"))]
    (let [t (trie/build (line-seq rdr))]
      (with-open [wrtr (io/writer (str root "/resources/public/words.json"))]
        (.write wrtr (json/write-str t)))))
  (println "Done."))
  
