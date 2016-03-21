(ns bookshelf.similar
  (:require [cljs.core.async :as async :refer [put!]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn link [id]
  (str "https://www.goodreads.com/book/show/" id))

(defn to-display [expanded similar]
  (if expanded
    similar
    (take 5 similar)))

(defn handle-add [_ book owner select-ch]
  (om/set-state! owner :added true)
  (put! select-ch book))

(defn similar-book [book owner {:keys [existing]}]
  (reify
    om/IInitState
    (init-state [_] {:added (boolean ((set existing) (:book/id book)))})
    om/IRenderState
    (render-state [_ {:keys [added]}]
      (let [select-ch (om/get-shared owner :select-ch)]
        (html
         [:div
          [:div.inline.similar.title
           [:a.book-link {:href (link (:book/id book))} (:book/title book)]]
          [:div.inline.similar.author (:book/author book)]
          [:button
           {:disabled added
            :on-click #(handle-add % book owner select-ch)}
           (if added "Added" "Add")]])))))

(defn similar [similar owner {:keys [existing]}]
  (reify
    om/IInitState
    (init-state [_] {:expanded false})
    om/IRenderState
    (render-state [_ {:keys [expanded]}]
      (html
       [:div
        [:h3 "Similar"]
        (om/build-all similar-book
                      (to-display expanded similar)
                      {:opts {:existing existing}})
        [:span.clickable
         {:style {:display (when expanded "none")}
          :on-click #(om/set-state! owner :expanded true)}
         "..."]]))))
