(ns bookshelf.shelf
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.xhr :as xhr]
            [bookshelf.routes :as routes]
            [bookshelf.search :as search]))

(defn edit-book [id title]
  (xhr/xhr {:method :put
            :url    (str "book/" id "/update")
            :data   {:book/title title}}))

(defn delete-book [id books]
  (om/transact! books [] #(vec (remove (fn [book] (= id (:book/id book))) %)))
  (xhr/xhr {:method :delete
            :url    (str "book/" id "/delete")}))

(defn book [book books]
  (html
   [:tr
    [:td
     [:a {:href (routes/book-path {:id (:book/id book)})}
      (:book/title book)]]
    [:td (:book/author book)]
    [:td
     [:button {:on-click #(delete-book (:book/id book) books)}
      "Delete"]]]))

(defn books [books]
  (html
   [:table
    [:tbody
     [:tr
      [:th "Title"]
      [:th "Author"]]
     (map #(book % books) books)]]))

(defn shelf [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (xhr/xhr {:method      :get
                :url         "books"
                :on-complete #(om/transact! app :books (fn [_] %))}))
    om/IRender
    (render [_]
      (html
       [:div#shelf
        [:h2 "Books"]
        (books (:books app))
        (om/build search/search app)]))))
