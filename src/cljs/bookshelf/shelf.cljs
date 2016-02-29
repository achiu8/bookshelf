(ns bookshelf.shelf
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [bookshelf.xhr :as xhr]))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn handle-change [e data edit-key owner]
  (om/transact! data edit-key #(.. e -target -value)))

(defn on-edit [id title]
  (xhr/xhr
   {:method      :put
    :url         (str "book/" id "/update")
    :data        {:book/title title}
    :on-complete (fn [res]
                   (println "server response:" res))}))

(defn end-edit [text owner cb]
  (om/set-state! owner :editing false)
  (cb text))

(defn add-book [id title]
  (xhr/xhr
   {:method      :post
    :url         "books"
    :data        {:book/amazon-id id :book/title title}
    :on-complete (fn [res]
                   (println "server response:" res))}))

(defn create-book [books owner]
  (let [book-id-el   (om/get-node owner "book-id")
        book-id      (.-value book-id-el)
        book-name-el (om/get-node owner "book-name")
        book-name    (.-value book-name-el)
        new-book     {:book/amazon-id book-id :book/title book-name}]
    (om/transact! books [] #(conj % new-book))
    (set! (.-value book-id-el) "")
    (set! (.-value book-name-el) "")
    (add-book book-id book-name)))

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

(defn shelf [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (xhr/xhr
       {:method      :get
        :url         "books"
        :on-complete #(om/transact! app :books (fn [_] %))}))
    om/IRender
    (render [_]
      (dom/div
       #js {:id "books"}
       (dom/h2 nil "Books")
       (apply dom/ul
              nil
              (map (fn [book]
                     (let [id (:book/amazon-id book)]
                       (om/build editable
                                 book
                                 {:opts {:edit-key :book/title
                                         :on-edit  #(on-edit id %)}})))
                   (:books app)))
       (dom/div
        nil
        (dom/label nil "ID:")
        (dom/input #js {:ref "book-amazon-id"})
        (dom/label nil "Name:")
        (dom/input #js {:ref "book-name"})
        (dom/button
         #js {:onClick #(create-book (:books app) owner)}
         "Add"))))))

