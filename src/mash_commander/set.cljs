(ns mash-commander.set
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [mash-commander.mode :as mode]
            [mash-commander.trie :as trie]
            [mash-commander.speech :as speech]
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
             :default word)))))

(defn- load [set]
  (let [words (load-words set)
        word-trie (trie/build words)]
    {:set set :trie word-trie}))

(defmethod mode/dispatch-keydown :set
  [cursor owner e]
  (let [key (.-key e)]
    (om/transact!
     (om/observe owner (mash-state/lines))
     #(let [trie (get-in % [:active :trie])]
        (cond
          ;; Backspace
          (= "Backspace" key)
          (if (empty? (get-in % [:active :letters])) %
              (as-> % c
                (assoc-in c [:active :trie] (first (get-in c [:active :trie-stack])))
                (assoc-in c [:active :trie-stack] (rest (get-in c [:active :trie-stack])))
                (assoc-in c [:active :letters] (rest (get-in c [:active :letters])))))
          ;; Ignore invalid transitions
          (not (contains? trie key)) %
          ;; Valid transition
          :default
          (do
            (when (contains? (get trie key) "")
              (go (>! speech/say (str/join (reverse (cons key (get-in % [:active :letters])))))))
            (as-> % c
              (assoc-in c [:active :trie-stack] (cons trie (get-in c [:active :trie-stack])))
              (assoc-in c [:active :trie] (get trie key))
              (assoc-in c [:active :letters] (cons key (get-in c [:active :letters]))))))))))

(defmethod mode/line-render-state :set
  [cursor owner state]
  (let [words (str/join (reverse (:letters cursor)))
        command-prompt (dom/span #js {:style #js {:color "#33f"
                                                  :fontWeight "bold"}}
                                 (str (:set cursor) " $ "))
        cursor-char (dom/span #js {:style #js {:color "#900"}} "\u2588")]
    (dom/div #js {:style #js {:fontSize "30px"
                              :lineHeight "40px"
                              :padding "15px 15px 0 15px"}
                  :onClick #(when-not (:focus state) (go (>! (om/get-shared owner :set-line) cursor)))}
             command-prompt
             (if (contains? (:trie cursor) "")
               (dom/span #js {:style #js {:color "#0f0"}} words)
               (dom/span #js {:style #js {:color "#080"}} words))
             cursor-char)))
