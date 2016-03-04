(ns ^:figwheel-always bookshelf.core
    (:require [om.core :as om :include-macros true]
              [bookshelf.routes :as routes]
              [bookshelf.app :as app]))

(enable-console-print!)

(def app-state (atom {:books []}))

(def target (.getElementById js/document "app"))

(def history-container (.getElementById js/document "history"))

(routes/define-routes! app-state history-container)

(om/root app/app app-state {:target target})
