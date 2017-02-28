(ns mash-commander.mode)

(defmulti dispatch-keydown
  (fn [cursor owner e]
    (get-in cursor [:lines :active :mode])))

(defmulti line-render-state
  (fn [cursor owner state]
    (get-in cursor [:mode])))
