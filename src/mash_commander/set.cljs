(ns mash-commander.set
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [mash-commander.mode :as mode]
            [mash-commander.trie :as trie]
            [mash-commander.speech :as speech]
            [mash-commander.image :as image]
            [mash-commander.state :as mash-state]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as str]))

(defn- load-words [set]
  (let [words (map #(get-in % ["when" "type"]) set)]
    (map (fn [word]
           (cond
             (empty? word) {:error "Word must not be empty."}
             (not (re-matches #"^[a-z]+( [a-z]+)*$" word))
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
        word-trie (get (trie/build words) "")
        actions (load-actions set)]
    {:actions actions :trie word-trie}))

(defn- do-action [set-name action]
  (let [say-phrase (get-in action ["say" "phrase"])
        show-picture-url (get-in action ["show" "picture" "url"])
        show-picture-file (get-in action ["show" "picture" "file"])]
    ;; Say
    (when-not (nil? say-phrase)
      (go (>! speech/say say-phrase)))
    ;; Show
    (let [url (cond
                (not (nil? show-picture-url)) show-picture-url
                (not (nil? show-picture-file)) (str "/sets/" set-name "/" show-picture-file)
                :default nil)]
      (when-not (nil? url)
        (go (>! image/show url))))))

(defmethod mode/dispatch-keydown :set
  [cursor owner e]
  (let [key (.-key e)]
    (om/transact!
     (om/observe owner (mash-state/lines))
     #(let [trie (get-in % [:active :trie])
            sets (om/observe owner (mash-state/sets))]
        (cond
          ;; Backspace
          (= "Backspace" key)
          (if (empty? (get-in % [:active :letters])) %
              (as-> % c
                (assoc-in c [:active :trie] (first (get-in c [:active :trie-stack])))
                (assoc-in c [:active :trie-stack] (rest (get-in c [:active :trie-stack])))
                (assoc-in c [:active :letters] (rest (get-in c [:active :letters])))))
          ;; Space
          (= " " key)
          (do
            (when (contains? trie "")
              (let [current-set (get sets (get-in % [:active :set]))
                    action (str/join (reverse (get-in % [:active :letters])))]
                (do-action (get-in % [:active :set]) (get-in current-set [:actions action]))))
            %)
          ;; Enter
          (= "Enter" key)
          (if (contains? trie "")
            (as-> % c
              (assoc c :history (cons (:active c) (:history c)))
              (assoc c :active (mash-state/initial-line-state-set owner (get-in c [:active :set]))))
            %)
          ;; Ignore invalid transitions
          (not (contains? trie key)) %
          ;; Valid transition
          :default
          (do
            (when (contains? (get trie key) "")
              (let [current-set (get sets (get-in % [:active :set]))
                    action (str/join (reverse (cons key (get-in % [:active :letters]))))]
                (do-action (get-in % [:active :set]) (get-in current-set [:actions action]))))
            (as-> % c
              (assoc-in c [:active :trie-stack] (cons trie (get-in c [:active :trie-stack])))
              (assoc-in c [:active :trie] (get trie key))
              (assoc-in c [:active :letters] (cons key (get-in c [:active :letters]))))))))))

(defmethod mode/line-render-state :set
  [cursor owner state]
  (let [words (str/join (reverse (:letters cursor)))
        rendered-words               (if (contains? (:trie cursor) "")
                                       (dom/span #js {:style #js {:color "#0f0"}} words)
                                       (dom/span #js {:style #js {:color "#080"}} words))
        command-prompt (dom/span #js {:style #js {:color "#33f"
                                                  :fontWeight "bold"}}
                                 (str (:set cursor) " $ "))
        cursor-char (dom/span #js {:style #js {:color "#900"}} "\u2588")]
    (apply dom/div #js {:style #js {:fontSize "30px"
                                    :lineHeight "40px"
                                    :padding "15px 15px 0 15px"}
                        :onClick #(when-not (:focus state) (go (>! (om/get-shared owner :set-line) cursor)))}
           (if (:focus state)
             [command-prompt
              rendered-words
              cursor-char]
             [rendered-words]))))
