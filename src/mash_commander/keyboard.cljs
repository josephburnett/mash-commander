(ns mash-commander.keyboard
  (:require [mash-commander.mode :as mode]
            [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn- key-view [cursor owner key-set letter-spec]
  (let [letter (:letter letter-spec)]
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
                            :color (if (contains? key-set letter)
                                     "#0b0" "#444")}
                           (:style letter-spec)))
                  :onMouseDown #(when (contains? key-set letter)
                                  (mode/dispatch-keydown cursor owner
                                                         (condp = letter
                                                           "Esc" "Escape"
                                                           letter)))}
             letter)))

(defn- row-view [cursor owner letters offset key-set]
  (apply dom/div #js {:style #js {:float "left"
                                  :clear "both"
                                  :paddingLeft offset}}
         (map (partial key-view cursor owner key-set) letters)))

(defn- standard-keys [letters]
  (map #(assoc {:style {}} :letter %)
       (seq letters)))

(defn keyboard-view [cursor owner]
  (reify
    om/IRender
    (render [_]
      (when (= :set (get-in cursor [:active :mode]))
        (let [key-set (set (keys (get-in cursor [:active :trie])))
              key-set (conj key-set "Esc")
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
                            (rv (cons {:letter "Esc" :style {:width "80px"
                                                             :margin-right "20px"}}
                                      (standard-keys "1234567890")) "0px" key-set)
                            (rv (standard-keys "qwertyuiop") "130px" key-set)
                            (rv (concat
                                 (standard-keys "asdfghjkl")
                                 [{:letter "Enter" :style {:width "120px"
                                                           :margin-left "50px"}}]) "150px" key-set)
                            (rv (standard-keys "zxcvbnm") "170px" key-set)
                            (rv [{:letter "Space" :style {:width "295px"}}] "290px" key-set))))))))
