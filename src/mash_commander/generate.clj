(ns mash-commander.generate
  (:require [mash-commander.trie :as trie]
            [mash-commander.nix.story :as nix-story]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [amazonica.aws.polly :as polly]
            [digest]))

(defn- nix-phrases [page]
  (flatten
   (cond
     (and (:say page) (:then page)) (cons (:say page) (nix-phrases (:then page)))
     (:say page) [(:say page)]
     (:then page) (nix-phrases (:then page)))))

(defn- set-phrases [root]
  (let [set-files (filter #(str/ends-with? (.getName %) ".json")
                          (file-seq (io/file (str root "/resources/public/sets"))))
        sets (map (comp json/read-str slurp #(.getPath %)) set-files)]
     (map #(get-in % ["when" "then" "say" "phrase"]) (flatten sets))))

(defn- all-phrases [root]
  (let [pages (map second (seq nix-story/pages))]
    (concat (flatten (map nix-phrases pages))
            (set-phrases root))))

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

(defn- gen-word-trie [root]
  (println "Generating word trie")
  (with-open [rdr (io/reader (str root "/words.txt"))]
    (let [t (trie/build (line-seq rdr))]
      (with-open [wrtr (io/writer (str root "/resources/public/cache/words:trie.json"))]
        (.write wrtr (json/write-str t))))))
  
(defn- cache-speech-phrases [root]
  (println "Caching speech files")
  (let [files (map (partial cache-phrase root) (all-phrases root))
        file-set (reduce (fn [coll val] (assoc coll val "")) {} files)]
    (with-open [wrtr (io/writer (str root "/resources/public/cache/speech:manifest.json"))]
      (.write wrtr (json/write-str file-set)))))

(defn- gen-set-manifest [root]
  (println "Generating set manifest")
  (let [set-manifest (map #(.getName %) (.listFiles (io/file "resources/public/sets")))]
    (with-open [wrtr (io/writer (str root "/resources/public/cache/set:manifest.json"))]
      (.write wrtr (json/write-str set-manifest)))))

(defn -main [_ root]
  (gen-word-trie root)
  (gen-set-manifest root)
  (cache-speech-phrases root)
  (println "Done."))
  
