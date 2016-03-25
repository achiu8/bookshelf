(ns bookshelf.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
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
      (om/build (pages app) app))))
