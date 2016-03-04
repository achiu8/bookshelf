(ns bookshelf.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(secretary/set-config! :prefix "#")

(defn define-routes! [app-state history-container]
  (defroute root-path "/" []
    (swap! app-state assoc :page :shelf))

  (defroute book-path "/books/:id" [id]
    (swap! app-state assoc :page :book :book-id id))
  
  (let [history (History. false nil history-container)]
    (goog.events/listen history
                        EventType/NAVIGATE
                        #(secretary/dispatch! (.-token %)))
    (doto history (.setEnabled true))))
