(ns bookshelf.similar
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn to-display [expanded similar]
  (if expanded
    similar
    (take 5 similar)))

(defn similar-book [book]
  (html
   [:div
    [:div.inline.similar-title (:book/title book)]
    [:div.inline.similar-author (:book/author book)]]))

(defn similar [similar owner]
  (reify
    om/IInitState
    (init-state [_] {:expanded false})
    om/IRenderState
    (render-state [_ {:keys [expanded]}]
      (html
       [:div
        [:h3 "Similar"]
        (map similar-book (to-display expanded similar))
        [:span.clickable
         {:style {:display (when expanded "none")}
          :on-click #(om/set-state! owner :expanded true)}
         "..."]]))))
