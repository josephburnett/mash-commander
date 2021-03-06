(ns mash-commander.freestyle.wolfram
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET]]
            [cljs.core.async :refer [chan put! close! <! >!]]
            [mash-commander.freestyle.command :as command]
            [mash-commander.speech :as speech]))

(defn- wolfram-handler [response]
  (go (>! speech/say response)))

(defn- wolfram-error-handler [{:keys [status status-text]}]
  (print "ERROR: wolfram" status status-text)
  (go (>! speech/say (str "Error! " status-text))))

(def ^:private wolfram
  (let [app-id (js/localStorage.getItem "wolfram-app-id")]
    (when-not (nil? app-id)
      (fn [question]
        (GET "http://api.wolframalpha.com/v1/result"
             {:params {:appid app-id
                       :i question}
              :handler wolfram-handler
              :error-handler wolfram-error-handler})))))

(def answer (chan))
(go-loop []
  (let [what (<! answer)]
    (if (nil? wolfram)
      (do
        (print "Wolfram needs an app id to ask:" what)
        (print "window.localStorage.setItem('wolfram-app-id', /* App Id /*)"))
      (wolfram what)))
  (recur))
