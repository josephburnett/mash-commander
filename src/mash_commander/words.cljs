(ns mash-commander.words
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan close!]]
            [ajax.core :refer [GET]]))
            
(def ^:private word-trie (atom {}))

(defn get-word-trie []
  @word-trie)

(defn init []
  (let [done (chan)]
    (go (GET "/cache/words:trie.json"
             {:params {:response-format :json
                       :keywords? false}
              :handler (fn [trie]
                         (swap! word-trie #(merge % trie))
                         (close! done))}))
    done))
