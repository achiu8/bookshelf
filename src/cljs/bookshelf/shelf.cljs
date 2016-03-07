(ns bookshelf.shelf
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.xhr :as xhr]
            [bookshelf.routes :as routes]
            [bookshelf.search :as search]
            [bookshelf.selectable :as selectable]))

(defn edit-book [id]
  (fn [key value]
    (xhr/xhr {:method :put
              :url    (str "books/" id "/update")
              :data   {key value}})))

(defn delete-book [id books]
  (om/transact! books [] #(vec (remove (fn [book] (= id (:book/id book))) %)))
  (xhr/xhr {:method :delete
            :url    (str "books/" id "/delete")}))

(defn book [book books]
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
                       :on-select  (edit-book (:book/id book))}})]
    [:td
     [:button {:on-click #(delete-book (:book/id book) books)}
      "Delete"]]]))

(defn sort-books [new-field owner]
  (let [old-field (om/get-state owner :sort-field)
        old-order (om/get-state owner :sort-order)]
    (om/set-state! owner :sort-field new-field)
    (if (= new-field old-field)
      (om/set-state! owner :sort-order (comp reverse old-order))
      (om/set-state! owner :sort-order (partial sort-by new-field)))))

(defn books [books owner]
  (reify
    om/IInitState
    (init-state [_]
      {:sort-field :book/title
       :sort-order (partial sort-by :book/title)})
    om/IRenderState
    (render-state [_ {:keys [sort-field sort-order]}]
      (html
       [:table
        [:tbody
         [:tr
          [:th.clickable {:on-click #(sort-books :book/title owner)} "Title"]
          [:th.clickable {:on-click #(sort-books :book/author owner)} "Author"]
          [:th.clickable "Status"]]
         (map #(book % books) (sort-order books))]]))))

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
        (om/build books (:books app))
        (om/build search/search app)]))))
