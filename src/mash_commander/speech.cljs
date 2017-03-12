(ns mash-commander.speech
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET]]
            [cljs-hash.md5 :as digest]
            [cljs.core.async :refer [chan put! close! <! >!]]))

(def ^:private speech-manifest (atom {}))
(go (GET "/cache/speech:manifest.json"
         {:params {:response-format :json
                   :keywords? :false}
          :handler (fn [word-set]
                     (reset! speech-manifest word-set))}))

(def ^:private polly
  (let [access-key-id (js/localStorage.getItem "aws-access-key-id")
        secret-access-key (js/localStorage.getItem "aws-secret-access-key")]
    (when-not (or (nil? access-key-id) (nil? secret-access-key))
      (js/AWS.Polly. #js {:apiVersion "2016-06-10"
                          :region "us-east-1"
                          :accessKeyId access-key-id
                          :secretAccessKey secret-access-key}))))

;; https://gist.github.com/msgodf/9296652
(defn- decode-audio-data
  [context data]
  (let [ch (chan)]
    (.decodeAudioData context
                      data
                      (fn [buffer]
                        (go (>! ch buffer)
                            (close! ch))))
    ch))

(def ^:private audio-context
  (let [AudioContext (or (.-AudioContext js/window)
                         (.-webkitAudioContext js/window))]
    (AudioContext.)))

(defn- play-audio [buffer]
  (go
    (let [decoded-buffer (<! (decode-audio-data audio-context buffer))
          source (doto (.createBufferSource audio-context)
                   (aset "buffer" decoded-buffer))]
      (.connect source (.-destination audio-context))
      (.start source 0))))

(defn- adhoc-say [what]
  (if (nil? polly)
    (do
      (print "Polly needs AWS credentials to say:" what)
      (print "window.localStorage.setItem('aws-access-key-id', /* access key id */)")
      (print "window.localStorage.setItem('aws-secret-access-key', /* secret access key */"))
    (. polly synthesizeSpeech (clj->js {:Text what
                                        :OutputFormat "mp3"
                                        :VoiceId "Ivy"})
       (fn [err data]
         (if err
           (print err err.stack)
           (let [buffer (.-buffer (.-AudioStream data))]
             (play-audio buffer)))))))

(defn- cache-say [what]
  (let [md5 (digest/md5 what)
        filename (str "/cache/speech:ivy:" md5 ".mp3")]
    (GET filename
         {:handler play-audio
          :response-format {:type :arraybuffer
                            :read #(.getResponse %)}})))

(def say (chan))
(go-loop []
  (let [what (<! say)]
    (if (contains? @speech-manifest (digest/md5 what))
      (cache-say what)
      (adhoc-say what))
    (recur)))

