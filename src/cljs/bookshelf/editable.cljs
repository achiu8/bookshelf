(ns bookshelf.editable
    (:require [om.core :as om :include-macros true]
              [sablono.core :as html :refer-macros [html]]
              [om.dom :as dom :include-macros true]))

(defn display [show]
  (if show {} {:display "none"}))

(defn handle-change [e data edit-key owner]
  (om/transact! data edit-key #(.. e -target -value)))

(defn end-edit [text owner cb]
  (om/set-state! owner :editing false)
  (cb text))

(defn editable [data owner {:keys [edit-key on-edit] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})
    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [text (get data edit-key)]
        (html
         [:li
          [:span {:style (display (not editing))} text]
          [:input
           {:style       (display editing)
            :value       text
            :on-change   #(handle-change % data edit-key owner)
            :on-key-down #(when (= (.-key %) "Enter")
                            (end-edit text owner on-edit))
            :on-blur     #(when (om/get-state owner :editing)
                            (end-edit text owner on-edit))}]
          [:button
           {:style   (display (not editing))
            :on-click #(om/set-state! owner :editing true)}
           "Edit"]])))))
