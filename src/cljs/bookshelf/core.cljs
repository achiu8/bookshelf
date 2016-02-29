(ns ^:figwheel-always bookshelf.core
    (:require [cljs.reader :as reader]
              [goog.events :as events]
              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true])
    (:import [goog.net XhrIo]
             goog.net.EventType
             [goog.events EventType]))

(enable-console-print!)

(def ^:private meths
  {:get    "GET"
   :put    "PUT"
   :post   "POST"
   :delete "DELETE"})

(defn edn-xhr [{:keys [method url data on-complete]}]
  (let [xhr (XhrIo.)]
    (events/listen xhr
                   goog.net.EventType.COMPLETE
                   #(on-complete (reader/read-string (.getResponseText xhr))))
    (. xhr
       (send url
             (meths method)
             (when data (pr-str data))
             #js {"Content-Type" "application/edn"}))))

(def app-state (atom {:classes []}))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

(defn handle-change [e data edit-key owner]
  (om/transact! data edit-key #(.. e -target -value)))

(defn on-edit [id title]
  (edn-xhr
   {:method      :put
    :url         (str "class/" id "/update")
    :data        {:class/title title}
    :on-complete (fn [res]
                   (println "server response:" res))}))

(defn end-edit [text owner cb]
  (om/set-state! owner :editing false)
  (cb text))

(defn add-class [id title]
  (edn-xhr
   {:method      :post
    :url         "classes"
    :data        {:class/id id :class/title title}
    :on-complete (fn [res]
                   (println "server response:" res))}))

(defn create-class [classes owner]
  (let [class-id-el   (om/get-node owner "class-id")
        class-id      (.-value class-id-el)
        class-name-el (om/get-node owner "class-name")
        class-name    (.-value class-name-el)
        new-class     {:class/id class-id :class/title class-name}]
    (om/transact! classes [] #(conj % new-class))
    (set! (.-value class-id-el) "")
    (set! (.-value class-name-el) "")
    (add-class class-id class-name)))

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

(defn classes-view [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (edn-xhr
       {:method      :get
        :url         "classes"
        :on-complete #(om/transact! app :classes (fn [_] %))}))
    om/IRender
    (render [_]
      (dom/div
       #js {:id "classes"}
       (dom/h2 nil "Classes")
       (apply dom/ul
              nil
              (map (fn [class]
                     (let [id (:class/id class)]
                       (om/build editable
                                 class
                                 {:opts {:edit-key :class/title
                                         :on-edit  #(on-edit id %)}})))
                   (:classes app)))
       (dom/div
        nil
        (dom/label nil "ID:")
        (dom/input #js {:ref "class-id"})
        (dom/label nil "Name:")
        (dom/input #js {:ref "class-name"})
        (dom/button
         #js {:onClick #(create-class (:classes app) owner)}
         "Add"))))))

(om/root classes-view
         app-state
         {:target (.getElementById js/document "classes")})
