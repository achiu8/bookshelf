(ns bookshelf.book
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.xhr :as xhr]))

(defn book-details [book]
  (html
   [:div
    [:p (:book/author book)]
    [:p (:book/rating book)]
    [:p (:book/year book)]
    [:p (str (:book/pages book) " pages")]
    [:div {:dangerouslySetInnerHTML {:__html (:book/description book)}}]]))


(defn book [{:keys [book-id books]} owner]
  (reify
    om/IRender
    (render [_]
      (let [book (some #(when (= book-id (:book/id %)) %) books)]
        (html
         [:div#book
          [:h2 (:book/title book)]
          (book-details book)])))))
