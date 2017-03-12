(ns mash-commander.keyboard
  (:require [mash-commander.mode :as mode]
            [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn key-view [cursor owner key-set letter]
  (dom/div #js {:style #js {:height "46px"
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
                            :color (if (contains? key-set letter)
                                     "#0b0" "#444")}
                :onMouseDown #(when (contains? key-set letter)
                                (mode/dispatch-keydown cursor owner letter))}
           letter))

(defn row-view [cursor owner letters offset key-set]
  (apply dom/div #js {:style #js {:float "left"
                                  :clear "both"
                                  :paddingLeft offset}}
         (map (partial key-view cursor owner key-set) letters)))

(defn keyboard-view [cursor owner]
  (reify
    om/IRender
    (render [_]
      (when (= :set (get-in cursor [:active :mode]))
        (let [key-set (set (keys (get-in cursor [:active :trie])))
              rv (partial row-view (:active cursor) owner)]
          (dom/div #js {:style #js {:position "absolute"
                                    :width "100vw"
                                    :heigth "100vh"}}
                   (dom/div #js {:style #js {:backgroundColor "#ddd"
                                             :width "600px"
                                             :height "25vm"
                                             :maxHeight "100%"
                                             :margin "70vh auto 0 auto"
                                             :zIndex "100"}}
                            (rv (seq "qwertyuiop") "0px" key-set)
                            (rv (seq "asdfghjkl") "20px" key-set)
                            (rv (seq "zxcvbnm") "40px" key-set))))))))
