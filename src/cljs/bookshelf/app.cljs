(ns bookshelf.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [bookshelf.shelf :as shelf]))

(defn app [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build shelf/shelf app))))
