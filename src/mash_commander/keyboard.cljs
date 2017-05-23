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
                           {:height "46px"
                            :width "56px"
                            :padding "10px 0 0 0"
                            :backgroundColor "#222"
                            :float "left"
                            :textAlign "center"
                            :fontSize "30px"
                            :borderWidth "2px"
                            :borderColor (if (= :command potential) "#227" "#000")
                            :borderStyle "solid"
                            :borderRadius "8px"
                            :color (cond
                                     (contains? #{:command :typing} potential) "#0b0"
                                     (= :mashing potential) "#060"
                                     :else "#444")}
                           (:style key-spec)))
                  :onMouseDown #(when-not (= :disabled potential)
                                  (mode/dispatch-keydown line-cursor owner
                                                         (condp = key
                                                           "Esc" "Escape"
                                                           "Space" " "
                                                           key)))}
             key)))

(defn- row-view [line-cursor owner key-list offset]
  (apply dom/div #js {:style #js {:float "left"
                                  :clear "both"
                                  :paddingLeft offset}}
         (map (partial key-view line-cursor owner)
              key-list)))

(defn- standard-keys [key-list]
  (map #(assoc {:style {}} :key %)
       (seq key-list)))

(defn keyboard-view [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [rv (partial row-view (:active cursor) owner)]
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
                                    (standard-keys "1234567890")) "0px")
                          (rv (standard-keys "qwertyuiop") "130px")
                          (rv (concat
                               (standard-keys "asdfghjkl")
                               [{:key "Enter" :style {:width "120px"
                                                      :marginLeft "50px"}}]) "150px")
                          (rv (standard-keys "zxcvbnm") "170px")
                          (rv [{:key "Space" :style {:width "295px"}}] "290px")))))))
