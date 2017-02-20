(ns mash-commander.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! close! <! >!]]
            [clojure.string :as str]
            [ajax.core :refer [GET]]))

(enable-console-print!)

(def polly
  (let [access-key-id (js/localStorage.getItem "aws-access-key-id")
        secret-access-key (js/localStorage.getItem "aws-secret-access-key")]
    (when-not (or (nil? access-key-id) (nil? secret-access-key))
      (js/AWS.Polly. #js {:apiVersion "2016-06-10"
                          :region "us-east-1"
                          :accessKeyId access-key-id
                          :secretAccessKey secret-access-key}))))

;; https://gist.github.com/msgodf/9296652
(defn decode-audio-data
  [context data]
  (let [ch (chan)]
    (.decodeAudioData context
                      data
                      (fn [buffer]
                        (go (>! ch buffer)
                            (close! ch))))
    ch))

(defn play-audio [buffer]
  (go
    (let [AudioContext (or (.-AudioContext js/window)
                           (.-webkitAudioContext js/window))
          context (AudioContext.)
          decoded-buffer (<! (decode-audio-data context buffer))
          source (doto (.createBufferSource context)
                   (aset "buffer" decoded-buffer))]
      (.connect source (.-destination context))
      (.start source 0))))

(def polly-say (chan))
(go-loop []
  (let [what (<! polly-say)]
    (if (nil? polly)
      (do
        (print "Polly needs AWS credentials to say:" what)
        (print "window.localStorage.setItem('accessKeyId', /* access key id */)")
        (print "window.localStorage.setItem('secretAccessKey', /* secret access key */"))
      (. polly synthesizeSpeech (clj->js {:Text what
                                          :OutputFormat "mp3"
                                          :VoiceId "Ivy"})
         (fn [err data]
           (if err
             (print err err.stack)
             (let [buffer (.-buffer (.-AudioStream data))]
               (play-audio buffer)))))))
    (recur))

(defn wolfram-handler [response]
  (go (>! polly-say response)))

(defn wolfram-error-handler [{:keys [status status-text]}]
  (print "ERROR: wolfram" status status-text)
  (go (>! polly-say (str "Error! " status-text))))

(def wolfram
  (let [app-id (js/localStorage.getItem "wolfram-app-id")]
    (when-not (nil? app-id)
      (fn [question]
        (GET "http://api.wolframalpha.com/v1/result"
             {:params {:appid app-id
                       :i question}
              :handler wolfram-handler
              :error-handler wolfram-error-handler})))))

(def wolfram-ask (chan))
(go-loop []
  (let [what (<! wolfram-ask)]
    (if (nil? wolfram)
      (do
        (print "Wolfram needs an app id to ask:" what)
        (print "window.localStorage.setItem('wolfram-app-id', /* App Id /*)"))
      (wolfram what)))
  (recur))

(defonce app-state
  (atom {:lines {:active {:state [:empty]
                          :letters []}
                 :history []}}))

(defn lines []
  (om/ref-cursor (:lines (om/root-cursor app-state))))

(def valid-words #{"hello" "world"})
(def valid-letters (set (concat (str/split "abcdefghijklmnopqrstuvwxyz" "")
                                    (str/split "ABCDEFGHIJKLMNOPQRSTUVWXYZ" "")
                                    (str/split "1234567890" ""))))

(defn recognize? [letters]
  (let [last-word (str/join (reverse (take-while #(not= " " %) letters)))]
    (contains? valid-words last-word)))

(def valid-commands #{"clear" "say" "wolfram"})

(defmulti dispatch-enter
  (fn [cursor]
    (let [letters (get-in cursor [:active :letters])
          command (str/join (take-while #(not= " " %) (reverse letters)))]
      (if (contains? valid-commands command)
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
    (assoc c :active {:state [:empty] :letters []})))49

(defmethod dispatch-enter "say"
  [cursor]
  (go (>! polly-say (apply str (drop 3 (reverse (get-in cursor [:active :letters]))))))
  (as-> cursor c
    (assoc c :history (cons (:active c) (:history c)))
    (assoc c :active {:state [:empty] :letters[]})))

(defmethod dispatch-enter "wolfram"
  [cursor]
  (go (>! wolfram-ask (apply str (drop 7 (reverse (get-in cursor [:active :letters]))))))
  (as-> cursor c
    (assoc c :history (cons (:active c) (:history c)))
    (assoc c :active {:state [:empty] :letters[]})))

(defn handle-keydown [owner e]
  (let [key (.-key e)]
    (om/transact!
     (om/observe owner (lines))
     #(let [state (first (get-in % [:active :state]))]
        (cond
          ;; Typing a letter
          (contains? valid-letters key)
          (as-> % c
            (assoc-in c [:active :letters] (cons key (get-in c [:active :letters])))
            (if (recognize? (:letters c))
              (assoc-in c [:active :state] (cons :typing (get-in c [:active :state])))
              (assoc-in c [:active :state] (cons :mashing (get-in c [:active :state])))))
          ;; Ignore leading and additional spaces
          (and (= " " key) (contains? #{:empty :typing-space :mashing-space} state)) %
          ;; Pressing first space
          (= " " key)
          (as-> % c
            (assoc-in c [:active :letters] (cons key (get-in c [:active :letters])))
            (if (= :typing state)
              (assoc-in c [:active :state] (cons :typing-space (get-in c [:active :state])))
              (assoc-in c [:active :state] (cons :mashing-space (get-in c [:active :state])))))
          ;; Ignore backspace on empty
          (and (= "Backspace" key) (= :empty state)) %
          ;; Backspace
          (= "Backspace" key)
          (as-> % c
            (assoc-in c [:active :state] (rest (get-in c [:active :state])))
            (assoc-in c [:active :letters] (rest (get-in c [:active :letters]))))
          ;; Enter
          (= "Enter" key) (dispatch-enter %)
          ;; Ignore everything else
          :default %)))))

(defn line-view [cursor owner]
  (reify
    om/IInitState
    (init-state [_] {:focus false})
    om/IDidMount
    (did-mount [_]
      (when (om/get-state owner :focus)
        (go-loop []
          (let [state (<! (om/get-shared owner :set-line))]
            (om/update! cursor state))
          (recur))))
    om/IRenderState
    (render-state [_ state]
      (let [words (str/split (str/join (reverse (:letters cursor))) " ")
            spacing (contains? #{:typing-space :mashing-space} (first (:state cursor)))
            commanding (contains? valid-commands (first words))
            cursor-char (dom/span #js {:style #js {:color "#900"}} "\u2588")
            rendered-words (interpose
                            (dom/span nil " ")
                            (map #(if (contains? valid-words %)
                                    (dom/span #js {:style #js {:color "#0f0"}} %)
                                    (dom/span #js {:style #js {:color "#080"}} %))
                                 words))]
        (apply dom/div #js {:style #js {:fontSize "30px"
                                        :lineHeight "40px"
                                        :padding "15px 15px 0 15px"}
                            :onClick #(when-not (:focus state) (go (>! (om/get-shared owner :set-line) cursor)))}
               (as-> rendered-words r
                 (if (and (:focus state) spacing) (concat r [(dom/span nil " ") cursor-char]) r)
                 (if (and (:focus state) (not spacing)) (concat r [cursor-char]) r)
                 (if commanding (cons (dom/span #js {:style #js {:color "#33f"}} (first words))
                                      (rest r)) r)))))))

(defn app-view [cursor owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (set! (.-onkeydown js/document.body) (partial handle-keydown owner)))
    om/IRender
    (render [_]
      (let [set-state (chan)]
        (apply dom/div #js {:style #js {:height "100vh"
                                        :width "100vw"
                                        :overflow "hidden"
                                        :padding "0"
                                        :margin "0"}}
               (cons
                (om/build line-view (get-in cursor [:lines :active]) {:state {:focus true}})
                (om/build-all line-view (get-in cursor [:lines :history]))))))))

(om/root app-view app-state
         {:target (. js/document (getElementById "app"))
          :shared {:set-line (chan)}})

