(ns bookshelf.actions
  (:require [om.core :as om :include-macros true]
            [bookshelf.xhr :as xhr]))

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
