(ns mash-commander.freestyle.command
  (:require [mash-commander.mode :as mode]
            [mash-commander.set.manifest :as set-manifest]
            [mash-commander.state :as mash-state]
            [mash-commander.trie :as trie]
            [om.core :as om :include-macros true]
            [clojure.string :as str]))

(def ^:private valid-commands (atom #{}))
(def ^:private built-in-command-trie (atom {}))

(defn add-command [name]
  (swap! valid-commands #(conj % name))
  (swap! built-in-command-trie #(trie/trie-merge % (trie/build @valid-commands))))

(defn get-command-trie []
  (trie/trie-merge @built-in-command-trie (set-manifest/get-set-trie)))

(defn valid-command? [name]
  (contains? @valid-commands name))

(defmulti dispatch-enter
  (fn [cursor owner]
    (let [letters (get-in cursor [:active :letters])
          command (str/join (take-while #(not= " " %) (reverse letters)))]
      (cond
        (valid-command? command) command
        (contains? @set-manifest/sets command) :set
        :default :default))))

(defmethod dispatch-enter :default
  [cursor _]
  (condp = (first (get-in cursor [:active :state]))
    :empty cursor ; ignore empty lines
    (as-> cursor c
      (assoc c :history (cons (:active c) (:history c)))
      (assoc c :active (mode/initial-line-state {:mode :freestyle})))))

(defmethod dispatch-enter :set
  [cursor owner]
  (let [letters (get-in cursor [:active :letters])
        set-name (str/join (take-while #(not= " " %) (reverse letters)))]
    (as-> cursor c
      (assoc c :history (cons (:active c) (:history c)))
      (assoc c :active (mode/initial-line-state {:mode :freestyle}))
      (assoc c :prev-history (:history c))
      (assoc c :prev-active (:active c))
      (assoc c :history [])
      (assoc c :active (mode/initial-line-state {:mode :set
                                                 :set set-name})))))

(defmethod dispatch-enter "clear"
  [cursor _]
  (as-> cursor c
    (assoc c :history [])
    (assoc c :active (mode/initial-line-state {:mode :freestyle}))))
(add-command "clear")

