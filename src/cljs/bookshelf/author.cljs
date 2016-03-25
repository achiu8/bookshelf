(ns bookshelf.author
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn author [{:keys [author]} owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div#book
        [:h2 (:author/name author)]
        [:p {:dangerouslySetInnerHTML {:__html (:author/about author)}}]]))))
