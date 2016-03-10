(ns bookshelf.shelf
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<! put! chan]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.routes :as routes]
            [bookshelf.actions :as actions]
            [bookshelf.search :as search]
            [bookshelf.selectable :as selectable]))

(defn sort-books [new-field owner]
  (let [old-field (om/get-state owner :sort-field)
        old-order (om/get-state owner :sort-order)]
    (om/set-state! owner :sort-field new-field)
    (if (= new-field old-field)
      (om/set-state! owner :sort-order (comp reverse old-order))
      (om/set-state! owner :sort-order (partial sort-by new-field)))))

(defn book [book delete-ch]
  (html
   [:tr
    [:td
     [:a {:href (routes/book-path {:id (:book/id book)})}
      (:book/title book)]]
    [:td (:book/author book)]
    [:td
     (om/build selectable/selectable
               book
               {:opts {:select-key :book/status
                       :on-select  (actions/edit-book book)}})]
    [:td
     [:button {:on-click #(put! delete-ch (:book/id book))}
      "Delete"]]]))

(defn books [books owner]
  (reify
    om/IInitState
    (init-state [_]
      {:sort-field :book/title
       :sort-order (partial sort-by :book/title)
       :delete-ch  (chan)})
    om/IWillMount
    (will-mount [_]
      (let [delete-ch (om/get-state owner :delete-ch)]
        (go (while true
              (let [book-id (<! delete-ch)]
                (actions/delete-book book-id books))))))
    om/IRenderState
    (render-state [_ {:keys [sort-field sort-order delete-ch]}]
      (html
       [:table
        [:tbody
         [:tr
          [:th.clickable.column {:on-click #(sort-books :book/title owner)} "Title"]
          [:th.clickable.column {:on-click #(sort-books :book/author owner)} "Author"]
          [:th.clickable.column {:on-click #(sort-books :book/status owner)} "Status"]]
         (map #(book % delete-ch) (sort-order books))]]))))

(defn shelf [app owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div#shelf
        [:h2 "Books"]
        (om/build books (:books app))
        (om/build search/search app)]))))
