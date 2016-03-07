(ns bookshelf.selectable
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn handle-change [e data select-key on-select owner]
  (let [selected (.. e -target -value)]
    (om/transact! data select-key (fn [_] selected))
    (on-select select-key selected)))

(defn selectable [data owner {:keys [select-key on-select] :as opts}]
  (reify
    om/IRender
    (render [_]
      (let [text (get data select-key)]
        (html
         [:div
          [:span text]
          [:select
           {:value       text
            :on-change   #(handle-change % data select-key on-select owner)}
           [:option "Unread"]
           [:option "Reading"]
           [:option "Read"]]])))))
