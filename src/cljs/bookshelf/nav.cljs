(ns bookshelf.nav
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.routes :as routes]))

(defn nav [app owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        [:a.link {:href (routes/root-path)} "Home"]]))))
