(ns bookshelf.book
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.xhr :as xhr]))

(defn book-details [book]
  (html
   [:div
    [:p (:id book)]
    [:p (:title book)]
    [:p (:author book)]
    [:p (:description book)]
    [:p (:rating book)]
    [:p (:pages book)]
    [:p (:isbn book)]
    [:p (:year book)]]))


(defn book [{:keys [book-id books]} owner]
  (let [book (some #(when (= book-id (:book/id %)) %) books)]
    (reify
      om/IInitState
      (init-state [_] {:details nil})
      om/IWillMount
      (will-mount [_]
        (xhr/xhr {:method      :get
                  :url         (str "books/" book-id)
                  :on-complete #(om/set-state! owner :details %)}))
      om/IRenderState
      (render-state [_ {:keys [details]}]
        (html
         [:div#book
          [:h2 (:title details)]
          (book-details details)])))))
