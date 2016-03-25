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

(defn added-books-container [books]
  (when (not-empty books)
    (html
     [:div
      [:h3 "Added"]
      (om/build-all book books)])))

(defn other-books-container [books any-added]
  (html
   [:div
    [:h3 (if any-added "Other" "Books")]
    (om/build-all book books)]))

(defn author [{:keys [author books]} owner]
  (reify
    om/IRender
    (render [_]
      (let [existing    (map :book/id books)
            added-books (filter #(= (:author/id author) (:book/author-id %)) books)
            other-books (remove #((set existing) (:book/id %)) (:author/books author))]
        (html
         [:div#book
          [:h2 (:author/name author)]
          [:p {:dangerouslySetInnerHTML {:__html (:author/about author)}}]
          (added-books-container added-books)
          (other-books-container other-books (not-empty added-books))])))))
