(ns bookshelf.similar
  (:require [cljs.core.async :as async :refer [put!]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn to-display [expanded similar]
  (if expanded
    similar
    (take 5 similar)))

(defn handle-add [_ book owner select-ch]
  (om/set-state! owner :added true)
  (put! select-ch book))

(defn similar-book [book owner]
  (reify
    om/IInitState
    (init-state [_] {:added false})
    om/IRenderState
    (render-state [_ {:keys [added]}]
      (let [select-ch (om/get-shared owner :select-ch)]
        (html
         [:div
          [:div.inline.similar-title (:book/title book)]
          [:div.inline.similar-author (:book/author book)]
          [:button
           {:disabled added
            :on-click #(handle-add % book owner select-ch)}
           (if added "Added" "Add")]])))))

(defn similar [similar owner]
  (reify
    om/IInitState
    (init-state [_] {:expanded false})
    om/IRenderState
    (render-state [_ {:keys [expanded]}]
      (html
       [:div
        [:h3 "Similar"]
        (om/build-all similar-book (to-display expanded similar))
        [:span.clickable
         {:style {:display (when expanded "none")}
          :on-click #(om/set-state! owner :expanded true)}
         "..."]]))))
