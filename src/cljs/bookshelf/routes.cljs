(ns bookshelf.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [bookshelf.xhr :as xhr])
  (:import goog.History))

(secretary/set-config! :prefix "#")

(defn nav-handler
  ([url page app-state args]
   (nav-handler url page nil app-state args))
  ([url page data app-state args]
   (xhr/xhr {:method      :get
             :url         url
             :data        data
             :on-complete #(apply swap! app-state assoc page % args)})))

(defn define-routes! [app-state history-container]
  (defroute root-path "/" []
    (nav-handler "/books" :books app-state [:page :shelf]))

  (defroute book-path "/books/:id" [id]
    (nav-handler "/books" :books app-state [:page :book :book-id id]))
  
  (defroute author-path "/authors/:id" [id]
    (nav-handler (str "/authors/" id) :author app-state [:page :author]))

  (let [history (History. false nil history-container)]
    (goog.events/listen history
                        EventType/NAVIGATE
                        #(secretary/dispatch! (.-token %)))
    (doto history (.setEnabled true))))
