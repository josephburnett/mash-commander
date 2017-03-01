(ns mash-commander.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [clojure.string :as str]
            [cljs.core.async :refer [chan]]
            [mash-commander.view :as view]
            [mash-commander.state :as state]
            [mash-commander.freestyle]
            [mash-commander.set]))

(enable-console-print!)

(om/root view/app-view state/app-state
         {:target (. js/document (getElementById "app"))
          :shared {:set-line (chan)}})

