(ns bookshelf.author
  (:require [cljs.core.async :as async :refer [put!]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.routes :as routes]))

(defn handle-add [_ book select-ch]
  (put! select-ch book))

(defn book [book]
  (html
   [:div
    [:a.link {:href (routes/book-path {:id (:book/id book)})}
     (:book/title book)]]))

(defn addable-book [book select-ch]
  (html
   [:div
    [:div.inline.title (:book/title book)]
    [:button
     {:on-click #(handle-add % book select-ch)}
     "Add"]]))

(defn added-books-container [books]
  (when (not-empty books)
    (html
     [:div
      [:h3 "Added"]
      (map book books)])))

(defn other-books-container [books any-added select-ch]
  (html
   [:div
    [:h3 (if any-added "Other" "Books")]
    (map #(addable-book % select-ch) books)]))

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
          (other-books-container other-books
                                 (not-empty added-books)
                                 (om/get-shared owner :select-ch))])))))
