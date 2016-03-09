(ns bookshelf.similar
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<! put! chan]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn similar-book [book]
  (html
   [:div (str (:book/title book) " - " (:book/author book))]))

(defn similar [similar owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        [:h3 "Similar"]
        (map similar-book similar)]))))
