(ns bookshelf.author
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.xhr :as xhr]
            [bookshelf.actions :as actions]
            [bookshelf.selectable :as selectable]
            [bookshelf.similar :as similar]))

(defn author [{:keys [author]} owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div#book
        [:h2 (:author/name author)]
        [:p (:author/about author)]]))))
