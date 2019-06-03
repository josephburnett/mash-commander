(ns mash-commander.state
  (:require [mash-commander.mode :as mode]
            [mash-commander.set.manifest :as set-manifest]
            [clojure.string :as string]
            [om.core :as om :include-macros true]))

(defonce app-state
  (atom {:lines {:history []}
         :words {}
         :characters {
                      :nix {:appearance {:state :resting
                                         :color "white"
                                         :speech-bubble []}
                            :fs {}
                            :page {:current-page :page-1}}}}))

(defn lines []
  (om/ref-cursor (:lines (om/root-cursor app-state))))

(defn words []
  (om/ref-cursor (:words (om/root-cursor app-state))))

(defn nix-appearance []
  (om/ref-cursor (get-in (om/root-cursor app-state) [:characters :nix :appearance])))

(defn load []
  (print "State loaded."))

; https://gist.github.com/kordano/56a16e1b28d706557f54
(defn url-params []
  (let [param-strs (-> (.-location js/window) (string/split #"\?") last (string/split #"\&"))]
    (into {} (for [[k v] (map #(string/split % #"=") param-strs)]
               [(keyword k) v]))))

(defn init []
  (let [set (:set (url-params))
        chroot (:chroot (url-params))]
    (cond
      ;; Start Nix
      (= "nix" chroot)
      (swap! app-state #(assoc-in % [:lines] {:active (mode/initial-line-state {:mode :nix})
                                              :history []}))
      ;; Start in a set
      (contains? @set-manifest/sets set)
      (swap! app-state #(assoc-in % [:lines]
                                  {:active (mode/initial-line-state {:mode :set
                                                                     :set set})
                                   :prev-active (mode/initial-line-state {:mode :freestyle})
                                   :prev-history []}))
      ;; Start in freestyle mode
      :default
      (swap! app-state #(assoc-in % [:lines :active]
                                  (mode/initial-line-state {:mode :freestyle}))))))

