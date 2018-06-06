(ns mash-commander.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [clojure.string :as str]
            [cljs.core.async :as async :refer [chan <!]]
            [mash-commander.nix.filesystem :as fs]
            [mash-commander.set.manifest :as set-manifest]
            [mash-commander.speech :as speech]
            [mash-commander.state :as state]
            [mash-commander.view :as view]
            [mash-commander.words :as words]))

(enable-console-print!)

(go
  (<! (async/merge
       [(set-manifest/init)
        (words/init)
        (speech/init)]))
  (state/init)
  (fs/init)
  (view/init))
