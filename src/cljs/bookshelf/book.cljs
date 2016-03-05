(ns bookshelf.book
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.xhr :as xhr]))

(defn book-details [book]
  (html
   [:div
    [:h4 (:book/author book)]]))


(defn book [{:keys [book-id books]} owner]
  (let [book (some #(when (= book-id (:book/id %)) %) books)]
    (reify
      om/IWillMount
      (will-mount [_]
        (xhr/xhr {:method      :get
                  :url         (str "book/" book-id)
                  :on-complete #(js/console.log %)}))
      om/IRender
      (render [_]
        (html
         [:div#book
          [:h2 (:book/title book)]
          (book-details book)])))))
