(ns bookshelf.actions
  (:require [om.core :as om :include-macros true]
            [bookshelf.xhr :as xhr]))

(defn get-similar [id book]
  (xhr/xhr {:method      :get
            :url         (str "books/" id "/similar")
            :on-complete #(om/set-state! book :similar %)}))

(defn edit-book [book]
  (fn [key value]
    (om/update! book key value)
    (xhr/xhr {:method :put
              :url    (str "books/" (:book/id book) "/update")
              :data   {key value}})))

(defn delete-book [id books]
  (om/update! books (vec (remove #(= id (:book/id %)) @books)))
  (xhr/xhr {:method :delete
            :url    (str "books/" id "/delete")}))

(defn select-result [selected app]
  (let [selected-id  (:book/id selected)
        existing-ids (map :book/id (:books @app))]
    (when-not ((set existing-ids) selected-id)
      (xhr/xhr {:method      :post
                :url         "books"
                :data        selected
                :on-complete (fn [res] (swap! app update :books #(conj % res)))}))))

(defn submit-search [owner]
  (let [search-term-input (om/get-node owner "search-term")
        search-term       (.-value search-term-input)]
    (when-not (empty? search-term)
      (xhr/xhr {:method      :get
                :url         (str "search/" search-term)
                :on-complete #(om/set-state! owner :results %)}))))
