(ns ^:figwheel-always bookshelf.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [cljs.core.async :as async :refer [alts! chan]]
              [om.core :as om :include-macros true]
              [bookshelf.actions :as actions]
              [bookshelf.routes :as routes]
              [bookshelf.app :as app]))

(enable-console-print!)

(defonce app-state
  (atom
   {:page      :shelf
    :book-id   nil
    :books     []
    :select-ch (chan)}))

(def target (.getElementById js/document "app"))

(def history-container (.getElementById js/document "history"))

(defn main [app target history-container]
  (let [select-ch (:select-ch @app)]
    (routes/define-routes! app-state history-container)
    (om/root app/app
             app-state
             {:target target
              :shared {:select-ch select-ch}})
    (go (while true
          (let [[v ch] (alts! [select-ch])]
            (condp = ch
              select-ch (actions/select-result v app)))))))

(main app-state target history-container)
