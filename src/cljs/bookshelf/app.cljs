(ns bookshelf.app
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [bookshelf.shelf :as shelf]))

(defn app [app owner]
  (shelf/shelf app owner))
