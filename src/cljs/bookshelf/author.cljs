(ns bookshelf.author
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn book [book]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        [:div (:book/title book)]]))))

(defn author [{:keys [author books]} owner]
  (reify
    om/IRender
    (render [_]
      (println author)
      (let [added-books (filter #(= (:author/id author) (:book/author-id %)) books)]
        (html
         [:div#book
          [:h2 (:author/name author)]
          [:p {:dangerouslySetInnerHTML {:__html (:author/about author)}}]
          [:h3 "Added"]
          (om/build-all book added-books)
          [:h3 "Other"]
          (om/build-all book (:author/books author))])))))
