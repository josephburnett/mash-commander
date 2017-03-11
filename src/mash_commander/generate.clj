(ns mash-commander.generate
  (:require [mash-commander.trie :as trie]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [amazonica.aws.polly :as polly]
            [digest]))

(defn- phrases [root]
  (let [set-files (filter #(str/ends-with? (.getName %) ".json")
                          (file-seq (io/file (str root "/resources/public/sets"))))
        sets (map (comp json/read-str slurp #(.getPath %)) set-files)]
    (map #(get-in % ["when" "then" "say" "phrase"]) (flatten sets))))

(defn- cache-phrase [root phrase]
  (let [md5 (digest/md5 phrase)
        filename (str root "/resources/public/cache/speech:ivy:" md5 ".mp3")]
    (when-not (.exists (io/file filename))
      (let [speech (polly/synthesize-speech :Text phrase
                                            :OutputFormat "mp3"
                                            :VoiceId "Ivy")]
        (println "caching" filename)
        (io/copy (.getWrappedInputStream (:audio-stream speech)) (io/file filename))))
    md5))

(defn -main [_ root]
  (println "Caching speech files")
  (let [files (map (partial cache-phrase root) (phrases root))
        file-set (reduce (fn [coll val] (assoc coll val "")) {} files)]
    (with-open [wrtr (io/writer (str root "/resources/public/cache/speech:manifest.json"))]
      (.write wrtr (json/write-str file-set))))
  (println "Generating word trie")
  (with-open [rdr (io/reader (str root "/words.txt"))]
    (let [t (trie/build (line-seq rdr))]
      (with-open [wrtr (io/writer (str root "/resources/public/cache/words:trie.json"))]
        (.write wrtr (json/write-str t)))))
  (println "Done."))
  
