(ns bookshelf.shelf
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [bookshelf.xhr :as xhr]
              [bookshelf.editable :as editable]
              [bookshelf.search :as search]))

(defn on-edit [id title]
  (xhr/xhr
   {:method      :put
    :url         (str "book/" id "/update")
    :data        {:book/title title}
    :on-complete (fn [res]
                   (println "server response:" res))}))

(defn book [book]
  (dom/tr
   nil
   (dom/td
    nil
    (dom/a
     #js {:href (str "https://www.goodreads.com/book/show/"
                     (:book/id book))}
     (:book/title book)))
   (dom/td nil (:book/author book))))

(defn books [books]
  (apply dom/table
         nil
         (dom/tr
          nil
          (dom/th nil "Title")
          (dom/th nil "Author"))
         (map book books)))

(defn shelf [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (xhr/xhr
       {:method      :get
        :url         "books"
        :on-complete #(om/transact! app :books (fn [_] %))}))
    om/IRender
    (render [_]
      (dom/div
       #js {:id "books"}
       (dom/h2 nil "Books")
       (books (:books app))
       (om/build search/search app)))))
