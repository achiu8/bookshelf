(ns bookshelf.xhr
    (:require [cljs.reader :as reader]
              [goog.events :as events])
    (:import [goog.net XhrIo]
             goog.net.EventType
             [goog.events EventType]))

(def ^:private meths
  {:get    "GET"
   :put    "PUT"
   :post   "POST"
   :delete "DELETE"})

(defn xhr [{:keys [method url data on-complete]}]
  (let [xhr-io (XhrIo.)]
    (events/listen xhr-io
                   goog.net.EventType.COMPLETE
                   #(on-complete (reader/read-string (.getResponseText xhr-io))))
    (. xhr-io
       (send url
             (meths method)
             (when data (pr-str data))
             #js {"Content-Type" "application/edn"}))))

