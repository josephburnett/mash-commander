(ns mash-commander.set.set-mode
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [mash-commander.mode :as mode]
            [mash-commander.trie :as trie]
            [mash-commander.speech :as speech]
            [mash-commander.image :as image]
            [mash-commander.state :as mash-state]
            [mash-commander.set.manifest :as set-manifest]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as str]))

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

(defmethod mode/can-restore :set [_ _ _] true)

(defmethod mode/dispatch-keydown :set
  [cursor owner key]
  (om/transact!
   (om/observe owner (mash-state/lines))
   #(let [trie (get-in % [:active :trie])]
      (cond
        ;; Esc
        (= "Escape" key)
        (as-> % c
          (assoc c :active (:prev-active c))
          (assoc c :history (:prev-history c))
          (dissoc c :prev-active)
          (dissoc c :prev-history))
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
            (let [current-set (get @set-manifest/sets (get-in % [:active :set]))
                  action (str/join (reverse (get-in % [:active :letters])))]
              (do-action (get-in % [:active :set]) (get-in current-set [:actions action]))))
          %)
        ;; Enter
        (= "Enter" key)
        (if (contains? trie "")
          (as-> % c
            (assoc c :history (cons (:active c) (:history c)))
            (assoc c :active (mode/initial-line-state {:mode :set
                                                       :set (get-in c [:active :set])})))
          %)
        ;; Ignore invalid transitions
        (not (contains? trie key)) %
        ;; Valid transition
        :default
        (do
          (when (contains? (get trie key) "")
            (let [current-set (get @set-manifest/sets (get-in % [:active :set]))
                  action (str/join (reverse (cons key (get-in % [:active :letters]))))]
              (do-action (get-in % [:active :set]) (get-in current-set [:actions action]))))
          (as-> % c
            (assoc-in c [:active :trie-stack] (cons trie (get-in c [:active :trie-stack])))
            (assoc-in c [:active :trie] (get trie key))
            (assoc-in c [:active :letters] (cons key (get-in c [:active :letters])))))))))

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
    (if (:focus state)
      [command-prompt
       rendered-words
       cursor-char]
      [rendered-words])))

(defmethod mode/initial-line-state :set [state]
  (let [trie (get-in @set-manifest/sets [(:set state) :trie])]
    (merge state
           {:trie trie
            :trie-stack []
            :letters []})))

(defmethod mode/key-potential :set
  [line key]
  (let [word-trie (:trie line)]
    (cond
      ;; Typing potential
      (or (contains? word-trie key)
          (and (= "Enter" key)
               (contains? word-trie "")))
      :typing
      ;; Escape back to freestyle mode
      (= "Escape" key)
      :mashing
      ;; Backspace when not empty
      (and (not (empty? (:letters line)))
           (= "Backspace" key))
      :mashing
      ;; Space at a leaf node replays the node
      (and (contains? word-trie "")
           (= " " key))
      :mashing
      ;; Other keys disabled
      :else :disabled)))

(defn load []
  (print "Loading set mode."))

