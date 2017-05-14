(ns mash-commander.freestyle.command
  (:require [mash-commander.set.manifest :as set-manifest]
            [mash-commander.state :as mash-state]
            [om.core :as om :include-macros true]
            [clojure.string :as str]))

(def valid-commands (atom #{}))

(defmulti dispatch-enter
  (fn [cursor owner]
    (let [letters (get-in cursor [:active :letters])
          command (str/join (take-while #(not= " " %) (reverse letters)))]
      (cond
        (contains? @valid-commands command) command
        (contains? @set-manifest/sets command) :set
        :default :default))))

(defmethod dispatch-enter :default
  [cursor _]
  (condp = (first (get-in cursor [:active :state]))
    :empty cursor ; ignore empty lines
    (as-> cursor c
      (assoc c :history (cons (:active c) (:history c)))
      (assoc c :active (mash-state/initial-line-state)))))

(defmethod dispatch-enter :set
  [cursor owner]
  (let [letters (get-in cursor [:active :letters])
        set-name (str/join (take-while #(not= " " %) (reverse letters)))]
    (as-> cursor c
      (assoc c :history (cons (:active c) (:history c)))
      (assoc c :active (mash-state/initial-line-state))
      (assoc c :prev-history (:history c))
      (assoc c :prev-active (:active c))
      (assoc c :history [])
      (assoc c :active (mash-state/initial-line-state-set set-name)))))

(defmethod dispatch-enter "clear"
  [cursor _]
  (as-> cursor c
    (assoc c :history [])
    (assoc c :active (mash-state/initial-line-state))))
(swap! valid-commands #(conj % "clear"))

