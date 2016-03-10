(ns bookshelf.search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<! alts! put! chan timeout]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.actions :as actions]
            [bookshelf.utils :as utils]))

(defn handle-keyup [e owner]
  ((utils/throttle actions/submit-search owner) owner)
  (put! (om/get-state owner :input-ch) (.. e -target -value)))

(defn handle-select [_ result search select-ch]
  (om/set-state! search :results [])
  (put! select-ch result))

(defn search-result [result owner {:keys [search select-ch]}]
  (reify
    om/IInitState
    (init-state [_] {:hovered false})
    om/IRenderState
    (render-state [_ {:keys [hovered]}]
      (html
       [:div.clickable
        {:style         {:font-weight (when hovered "bold")}
         :on-click      #(handle-select % result search select-ch)
         :on-mouse-over #(om/set-state! owner :hovered true)
         :on-mouse-out  #(om/set-state! owner :hovered false)}
        (:title result)]))))

(defn search-results [results search select-ch]
  (html
   [:div
    (om/build-all search-result
                  results
                  {:opts {:search    search
                          :select-ch select-ch}})]))

(defn search [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:results   []
       :throttled false
       :debounced false
       :input-ch  (chan)})
    om/IWillMount
    (will-mount [_]
      (go (while true
            (let [input-ch (om/get-state owner :input-ch)
                  delay-ch (timeout 500)
                  [val ch] (alts! [input-ch delay-ch])]
              (condp = ch
                input-ch (om/set-state! owner :debounced true)
                delay-ch (when (om/get-state owner :debounced)
                           (actions/submit-search owner)
                           (om/set-state! owner :debounced false)))))))
    om/IRenderState
    (render-state [_ {:keys [results select-ch]}]
      (html
       [:div
        [:input
         {:ref       "search-term"
          :on-key-up #(handle-keyup % owner)}]
        (search-results results owner (:select-ch app))]))))
