(ns bookshelf.app
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.nav :as nav]
            [bookshelf.shelf :as shelf]
            [bookshelf.book :as book]
            [bookshelf.author :as author]))

(defn pages [{:keys [page]}]
  (condp = page
    :shelf  shelf/shelf
    :book   book/book
    :author author/author))

(defn app [app owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        (om/build nav/nav app)
        (om/build (pages app) app)]))))
