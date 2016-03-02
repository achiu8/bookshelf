(ns bookshelf.search
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [bookshelf.xhr :as xhr]))

(defn throttle [f owner]
  (fn [& args]
    (when-not (om/get-state owner :throttled)
      (apply f args)
      (om/set-state! owner :throttled true)
      (js/setTimeout #(om/set-state! owner :throttled false) 500))))

(defn submit-search [books owner]
  (let [search-term-input (om/get-node owner "search-term")
        search-term       (.-value search-term-input)]
    (when-not (empty? search-term)
      (xhr/xhr
       {:method      :get
        :url         (str "search/" search-term)
        :on-complete #(om/set-state! owner :results %)}))))

(defn select-result [{:keys [id title author]} app owner]
  (let [selected {:book/id id :book/title title :book/author author}]
    (om/transact! (:books app) [] #(conj % selected))
    (om/set-state! owner :results [])
    (xhr/xhr
     {:method      :post
      :url         "books"
      :data        selected
      :on-complete (fn [res]
                     (println "server response:" res))})))

(defn search-result [result app owner]
  (dom/div
   #js {:onClick #(select-result result app owner)}
   (:title result)))

(defn search-results [results app owner]
  (apply dom/div
         nil
         (map #(search-result % app owner) results)))

(defn search [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:results []
       :throttled false})
    om/IRenderState
    (render-state [_ {:keys [results]}]
      (dom/div
        nil
        (dom/input
         #js {:ref     "search-term"
              :onKeyUp #((throttle submit-search owner) (:books app) owner)})
        (dom/button
         #js {:onClick #(submit-search (:books app) owner)}
         "Search")
        (search-results results app owner)))))
