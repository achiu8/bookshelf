(ns bookshelf.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [bookshelf.xhr :as xhr])
  (:import goog.History))

(secretary/set-config! :prefix "#")

(defn navigation-handler [endpoint app-state args]
  (xhr/xhr {:method      :get
            :url         (name endpoint)
            :on-complete #(apply swap! app-state assoc :books % args)}))

(defn define-routes! [app-state history-container]
  (defroute root-path "/" []
    (navigation-handler :books app-state [:page :shelf]))

  (defroute book-path "/books/:id" [id]
    (navigation-handler :books app-state [:page :book :book-id id]))
  
  (let [history (History. false nil history-container)]
    (goog.events/listen history
                        EventType/NAVIGATE
                        #(secretary/dispatch! (.-token %)))
    (doto history (.setEnabled true))))
