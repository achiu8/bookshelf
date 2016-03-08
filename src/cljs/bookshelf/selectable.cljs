(ns bookshelf.selectable
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn handle-change [e select-key on-select owner]
  (on-select select-key (.. e -target -value)))

(defn display [hovered]
  (when hovered {:display "none"}))

(defn selectable [data owner {:keys [select-key on-select styles] :as opts}]
  (reify
    om/IInitState
    (init-state [_] {:hovered false})
    om/IRenderState
    (render-state [_ {:keys [hovered]}]
      (let [text (get data select-key)]
        (html
         [:div
          {:style         styles
           :on-mouse-over #(om/set-state! owner :hovered true)
           :on-mouse-out  #(om/set-state! owner :hovered false)}
          [:span {:style (display hovered)} text]
          [:select
           {:style     (assoc (display (not hovered)) :width "100%")
            :value     text
            :on-change #(handle-change % select-key on-select owner)}
           (for [option ["Unread" "Reading" "Read"]] [:option option])]])))))
