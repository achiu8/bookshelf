(ns bookshelf.editable
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

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
        (dom/li nil
                (dom/span #js {:style (display (not editing))} text)
                (dom/input
                 #js {:style     (display editing)
                      :value     text
                      :onChange  #(handle-change % data edit-key owner)
                      :onKeyDown #(when (= (.-key %) "Enter")
                                    (end-edit text owner on-edit))
                      :onBlur    #(when (om/get-state owner :editing)
                                    (end-edit text owner on-edit))})
                (dom/button
                 #js {:style   (display (not editing))
                      :onClick #(om/set-state! owner :editing true)}
                 "Edit"))))))
