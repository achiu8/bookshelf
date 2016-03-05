(ns bookshelf.book
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.xhr :as xhr]))

(defn book-details [book]
  (html
   [:div
    [:p (:book/id book)]
    [:p (:book/author book)]
    [:p (:book/description book)]
    [:p (:book/rating book)]
    [:p (:book/pages book)]
    [:p (:book/isbn book)]
    [:p (:book/year book)]]))


(defn book [{:keys [book-id books]} owner]
  (let [book (some #(when (= book-id (:book/id %)) %) books)]
    (reify
      om/IRender
      (render [_]
        (html
         [:div#book
          [:h2 (:book/title book)]
          (book-details book)])))))
