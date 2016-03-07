(ns bookshelf.selectable
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn handle-change [e data edit-key owner]
  (om/transact! data edit-key #(.. e -target -value)))

(defn selectable [data owner {:keys [select-key] :as opts}]
  (reify
    om/IRender
    (render [_]
      (let [text (get data select-key)]
        (html
         [:div
          [:span text]
          [:select
           {:value       text
            :on-change   #(handle-change % data select-key owner)}
           [:option "Unread"]
           [:option "Reading"]
           [:option "Read"]]])))))
