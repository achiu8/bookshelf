(ns bookshelf.selectable
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn handle-change [e data select-key on-select owner]
  (let [selected (.. e -target -value)]
    (om/transact! data select-key (fn [_] selected))
    (on-select select-key selected)))

(defn display [hovered]
  (when hovered {:display "none"}))

(defn selectable [data owner {:keys [select-key on-select] :as opts}]
  (reify
    om/IInitState
    (init-state [_] {:hovered false})
    om/IRenderState
    (render-state [_ {:keys [hovered]}]
      (let [text (get data select-key)]
        (html
         [:div
          {:on-mouse-over #(om/set-state! owner :hovered true)
           :on-mouse-out  #(om/set-state! owner :hovered false)}
          [:span {:style (display hovered)} text]
          [:select
           {:style       (assoc (display (not hovered)) :width "100%")
            :value       text
            :on-change   #(handle-change % data select-key on-select owner)}
           [:option "Unread"]
           [:option "Reading"]
           [:option "Read"]]])))))
