(ns bookshelf.search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<! put! chan]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.actions :as actions]
            [bookshelf.utils :as utils]))

(defn search-result [result owner {:keys [select-ch]}]
  (reify
    om/IInitState
    (init-state [_] {:hovered false})
    om/IRenderState
    (render-state [_ {:keys [hovered]}]
      (html
       [:div.clickable
        {:style         {:font-weight (when hovered "bold")}
         :on-click      #(put! select-ch result)
         :on-mouse-over #(om/set-state! owner :hovered true)
         :on-mouse-out  #(om/set-state! owner :hovered false)}
        (:title result)]))))

(defn search-results [results select-ch]
  (html
   [:div
    (om/build-all search-result
                  results
                  {:opts {:select-ch select-ch}})]))

(defn search [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:results   []
       :throttled false
       :select-ch (chan)})
    om/IWillMount
    (will-mount [_]
      (let [select-ch (om/get-state owner :select-ch)]
        (go (while true
              (let [result (<! select-ch)]
                (actions/select-result result app owner))))))
    om/IRenderState
    (render-state [_ {:keys [results select-ch]}]
      (html
       [:div
        [:input
         {:ref       "search-term"
          :on-key-up #((utils/throttle actions/submit-search owner) owner)}]
        (search-results results select-ch)]))))
