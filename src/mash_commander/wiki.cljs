(ns mash-commander.wiki
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET]]
            [cljs.core.async :refer [chan put! close! <! >!]]
            [mash-commander.command :as command]
            [mash-commander.speech :as speech]))

(defn- wiki-search-handler [return response]
  (let [title (get-in response ["query" "search" 0 "title"])]
    (go (>! return title))))

(defn- wiki-summary-handler [return response]
  (let [pages (get-in response ["query" "pages"])
        summary (get-in (second (first pages)) ["extract"])]
    (go (>! return summary))))

(defn- wiki-error-handler [{:keys [status status-text]}]
  (print "ERROR: wiki" status status-text)
  (go (>! speech/say (str "Error! " status-text))))

(defn- wiki-search [term]
  (let [return (chan)]
    (GET "https://en.wikipedia.org/w/api.php"
         {:params {:action "query"
                   :list "search"
                   :srprop "sectiontitle"
                   :srlimit "1"
                   :origin "*"
                   :format "json"
                   :srsearch term}
          :handler (partial wiki-search-handler return)
          :error-handler wiki-error-handler})
    (go
      (let [title (<! return)
            return (chan)]
        (GET "https://en.wikipedia.org/w/api.php"
             {:params {:action "query"
                       :prop "extracts"
                       :explaintext nil
                       :exintro nil
                       :origin "*"
                       :format "json"
                       :titles title}
              :handler (partial wiki-summary-handler return)
              :error-handler wiki-error-handler})
        (let [summary (<! return)]
          (go (>! speech/say (subs summary 0 200)))))))) ; limit to 200 characters

(def search (chan))
(go-loop []
  (let [what (<! search)]
    (wiki-search what))
  (recur))

(defmethod command/dispatch-enter "wiki"
  [cursor]
  (go (>! search (apply str (drop 4 (reverse (get-in cursor [:active :letters]))))))
  (as-> cursor c
    (assoc c :history (cons (:active c) (:history c)))
    (assoc c :active {:state [:empty] :letters[]})))
(swap! command/valid-commands #(conj % "wiki"))
