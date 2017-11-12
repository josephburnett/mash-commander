(ns mash-commander.keyboard
  (:require [mash-commander.mode :as mode]
            [clojure.string :as str]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn- key-view [line-cursor owner key-spec]
  (let [key (:key key-spec)
        potential (mode/key-potential line-cursor key)]
    (dom/div #js {:style (clj->js
                          (merge
                           {:height "4.5vw"
                            :width "5vw"
                            :padding "1.2vw 0 0 0"
                            :backgroundColor "#222"
                            :float "left"
                            :textAlign "center"
                            :fontSize "3vw"
                            :borderWidth "0.3vw"
                            :borderColor "#000"
                            :borderStyle "solid"
                            :borderRadius "1.2vw"
                            :color (condp = potential
                                     :command "#33f"
                                      :typing "#0b0"
                                     :mashing "#060"
                                     "#444")
                           :MozUserSelect "none"
                           :WebkitUserSelect "none"
                           :msUserSelect "none"}
                           (:style key-spec)))
                  :onMouseDown #(when-not (= :disabled potential)
                                  (mode/dispatch-keydown line-cursor owner key))}
             (:display key-spec))))

(defn- row-view [line-cursor owner key-list offset]
  (apply dom/div #js {:style #js {:float "left"
                                  :clear "both"
                                  :paddingLeft offset}}
         (map (partial key-view line-cursor owner)
              key-list)))

(defn- standard-keys [key-list]
  (map #(assoc {:style {}} :key % :display %)
       (seq key-list)))

(defn keyboard-view [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [rv (partial row-view (:active cursor) owner)]
        (dom/div #js {:style #js {:position "absolute"
                                  :bottom "7vh"
                                  :width "100vw"
                                  :heigth "100vh"}}
                 (dom/div #js {:style #js {:backgroundColor "#ddd"
                                           :width "90vw"
                                           :height "25vm"
                                           :maxHeight "100%"
                                           :margin "0 auto"
                                           :padding "0 0 0 3vw"
                                           :zIndex "100"}}
                          (rv (concat
                               [{:key "Escape" :display "Esc" :style {:width "8vw" :marginRight "2vw"}}]
                               (standard-keys "1234567890")
                               [{:key "Backspace" :display "<--" :style {:width "10vw" :marginLeft "8vw"}}]) "0vw")
                          (rv (standard-keys "qwertyuiop") "13vw")
                          (rv (concat
                               (standard-keys "asdfghjkl")
                               [{:key "Enter" :display "Enter":style {:width "15vw" :marginLeft "4vw"}}]) "15vw")
                          (rv (standard-keys "zxcvbnm") "18vw")
                          (rv [{:key " " :display "Space" :style {:width "27vw"}}] "29vw")))))))
