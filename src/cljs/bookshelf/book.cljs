(ns bookshelf.book
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.xhr :as xhr]))

(defn book [app owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div#book
        [:h2 (str "Hello " (:book-id app))]]))))
