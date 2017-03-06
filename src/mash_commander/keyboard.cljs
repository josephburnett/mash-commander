(ns mash-commander.keyboard
  (:require [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn key-view [key-set letter]
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
                                     "#0b0" "#444")}}
           letter))

(defn row-view [letters offset key-set]
  (apply dom/div #js {:style #js {:float "left"
                                  :clear "both"
                                  :paddingLeft offset}}
         (map (partial key-view key-set) letters)))

(defn keyboard-view [cursor owner]
  (reify
    om/IRender
    (render [_]
      (when (= :set (get-in cursor [:mode]))
        (let [key-set (set (keys (:trie cursor)))]
          (dom/div #js {:style #js {:position "absolute"
                                    :width "100vw"
                                    :heigth "100vh"}}
                   (dom/div #js {:style #js {:backgroundColor "#ddd"
                                             :width "600px"
                                             :height "25vm"
                                             :maxHeight "100%"
                                             :margin "70vh auto 0 auto"}}
                            (row-view (seq "qwertyuiop") "0px" key-set)
                            (row-view (seq "asdfghjkl") "20px" key-set)
                            (row-view (seq "zxcvbnm") "40px" key-set))))))))
