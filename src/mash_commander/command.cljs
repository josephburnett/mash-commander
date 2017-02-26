(ns mash-commander.command
  (:require [clojure.string :as str]))

(def valid-commands (atom #{"clear"}))

(defmulti dispatch-enter
  (fn [cursor]
    (let [letters (get-in cursor [:active :letters])
          command (str/join (take-while #(not= " " %) (reverse letters)))]
      (if (contains? @valid-commands command)
        command
        :default))))

(defmethod dispatch-enter :default
  [cursor]
  (condp = (first (get-in cursor [:active :state]))
    :empty cursor ; ignore empty lines
    (as-> cursor c
      (assoc c :history (cons (:active c) (:history c)))
      (assoc c :active {:state [:empty] :letters []}))))

(defmethod dispatch-enter "clear"
  [cursor]
  (as-> cursor c
    (assoc c :history [])
    (assoc c :active {:state [:empty] :letters []})))

