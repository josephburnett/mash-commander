(ns mash-commander.keyboard
  (:require [mash-commander.mode :as mode]
            [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn- key-view [cursor owner word-key-set command-key-set default-enabled key-spec]
  (let [key (:key key-spec)
        enabled (or (contains? word-key-set key)
                    (contains? command-key-set key)
                    default-enabled)]
    (dom/div #js {:style (clj->js
                          (merge
                           {:height "46px"
                            :width "56px"
                            :padding "10px 0 0 0"
                            :backgroundColor "#222"
                            :float "left"
                            :textAlign "center"
                            :fontSize "30px"
                            :borderWidth "2px"
                            :borderColor "#000"
                            :borderStyle "solid"
                            :borderRadius "8px"
                            :color (cond
                                     (contains? command-key-set key) "#33f"
                                     (contains? word-key-set key) "#0b0"
                                     default-enabled "#080"
                                     :else "#444")}
                           (:style key-spec)))
                  :onMouseDown #(when enabled
                                  (mode/dispatch-keydown cursor owner
                                                         (condp = key
                                                           "Esc" "Escape"
                                                           "Space" " "
                                                           key)))}
             key)))

(defn- row-view [cursor owner key-list offset word-key-set command-key-set default-enabled]
  (apply dom/div #js {:style #js {:float "left"
                                  :clear "both"
                                  :paddingLeft offset}}
         (map (partial key-view cursor owner word-key-set command-key-set default-enabled)
              key-list)))

(defn- standard-keys [key-list]
  (map #(assoc {:style {}} :key %)
       (seq key-list)))

(defn keyboard-view [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [word-key-set (conj (set (keys (get-in cursor [:active :trie]))) "Esc")
            command-key-set (if (= :freestyle (get-in cursor [:active :mode]))
                              (set (keys (get-in cursor [:active :command-trie]))) #{})
            default-enabled (= :freestyle (get-in cursor [:active :mode]))
            rv (partial row-view (:active cursor) owner)]
        (dom/div #js {:style #js {:position "absolute"
                                  :width "100vw"
                                  :heigth "100vh"}}
                 (dom/div #js {:style #js {:backgroundColor "#ddd"
                                           :width "870px"
                                           :height "25vm"
                                           :maxHeight "100%"
                                           :margin "60vh auto 0 auto"
                                           :zIndex "100"}}
                          (rv (cons {:key "Esc" :style {:width "80px"
                                                        :marginRight "20px"}}
                                    (standard-keys "1234567890")) "0px" word-key-set)
                          (rv (standard-keys "qwertyuiop") "130px" word-key-set)
                          (rv (concat
                               (standard-keys "asdfghjkl")
                               [{:key "Enter" :style {:width "120px"
                                                      :marginLeft "50px"}}]) "150px" word-key-set)
                          (rv (standard-keys "zxcvbnm") "170px" word-key-set)
                          (rv [{:key "Space" :style {:width "295px"}}] "290px" word-key-set)))))))
