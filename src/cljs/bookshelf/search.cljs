(ns bookshelf.search
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
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
      (xhr/xhr {:method      :get
                :url         (str "search/" search-term)
                :on-complete #(om/set-state! owner :results %)}))))

(defn select-result [{:keys [id title author]} app owner]
  (let [selected {:book/id id :book/title title :book/author author}]
    (om/set-state! owner :results [])
    (xhr/xhr {:method      :post
              :url         "books"
              :data        selected
              :on-complete (fn [res] (om/transact! (:books app) [] #(conj % res)))})))

(defn search-result [result owner {:keys [app parent]}]
  (reify
    om/IInitState
    (init-state [_] {:hovered false})
    om/IRenderState
    (render-state [_ {:keys [hovered]}]
      (html
       [:div.clickable
        {:style         {:font-weight (when hovered "bold")}
         :on-click      #(select-result result app parent)
         :on-mouse-over #(om/set-state! owner :hovered true)
         :on-mouse-out  #(om/set-state! owner :hovered false)}
        (:title result)]))))

(defn search-results [results app owner]
  (html
   [:div
    (om/build-all search-result
                  results
                  {:opts {:app app :parent owner}})]))

(defn search [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:results []
       :throttled false})
    om/IRenderState
    (render-state [_ {:keys [results]}]
      (html
       [:div
        [:input
         {:ref       "search-term"
          :on-key-up #((throttle submit-search owner) (:books app) owner)}]
        (search-results results app owner)]))))
