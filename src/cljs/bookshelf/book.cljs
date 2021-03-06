(ns bookshelf.book
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.xhr :as xhr]
            [bookshelf.actions :as actions]
            [bookshelf.selectable :as selectable]
            [bookshelf.similar :as similar]
            [bookshelf.routes :as routes]))

(defn book-details [book]
  (html
   [:div
    [:div
     [:span "Status: "]
     [:span (om/build selectable/selectable
                      book
                      {:opts {:select-key :book/status
                              :on-select  (actions/edit-book book)
                              :styles     {:display "inline-block"}}})]]
    [:p
     [:a.link {:href (routes/author-path {:id (:book/author-id book)})}
      (:book/author book)]]
    [:p (:book/rating book)]
    [:p (:book/year book)]
    [:p (str (:book/pages book) " pages")]
    [:div {:dangerouslySetInnerHTML {:__html (:book/description book)}}]]))


(defn book [{:keys [book-id books]} owner]
  (reify
    om/IInitState
    (init-state [_] {:similar []})
    om/IWillMount
    (will-mount [_]
      (actions/get-similar book-id owner))
    om/IRenderState
    (render-state [_ {:keys [similar]}]
      (let [book     (some #(when (= book-id (:book/id %)) %) books)
            existing (map :book/id books)]
        (html
         [:div#book
          [:h2 (:book/title book)]
          (book-details book)
          (om/build similar/similar
                    similar
                    {:opts {:existing existing}})])))))
