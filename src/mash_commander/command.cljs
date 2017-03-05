(ns mash-commander.command
  (:require [mash-commander.state :as mash-state]
            [om.core :as om :include-macros true]
            [clojure.string :as str]))

(def valid-commands (atom #{}))

(defmulti dispatch-enter
  (fn [cursor owner]
    (let [letters (get-in cursor [:active :letters])
          command (str/join (take-while #(not= " " %) (reverse letters)))]
      (if (contains? @valid-commands command)
        command
        :default))))

(defmethod dispatch-enter :default
  [cursor _]
  (condp = (first (get-in cursor [:active :state]))
    :empty cursor ; ignore empty lines
    (as-> cursor c
      (assoc c :history (cons (:active c) (:history c)))
      (assoc c :active (mash-state/initial-line-state)))))

(defmethod dispatch-enter "clear"
  [cursor _]
  (as-> cursor c
    (assoc c :history [])
    (assoc c :active (mash-state/initial-line-state))))
(swap! valid-commands #(conj % "clear"))

(defmethod dispatch-enter "set"
  [cursor owner]
  (let [letters (get-in cursor [:active :letters])
        set-name (str/join (reverse (take-while #(not= " " %) letters)))]
    (if (contains? (om/observe owner (mash-state/sets)) set-name)
      (as-> cursor c
        (assoc c :history [])
        (assoc c :active (mash-state/initial-line-state-set owner set-name)))
      cursor))) ;; Do nothing when set name is invalid
(swap! valid-commands #(conj % "set"))
