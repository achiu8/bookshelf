(ns ^:figwheel-always bookshelf.core
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [bookshelf.app :as app]))

(enable-console-print!)

(def app-state (atom {:books []}))

(om/root app/app
         app-state
         {:target (.getElementById js/document "app")})
