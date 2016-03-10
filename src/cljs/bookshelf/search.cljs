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
       :debounced false
       :select-ch (chan)
       :input-ch  (chan)})
    om/IWillMount
    (will-mount [_]
      (let [select-ch (om/get-state owner :select-ch)
            input-ch  (om/get-state owner :input-ch)]
        (go (while true
              (let [delay-ch (timeout 500)
                    [val ch] (alts! [select-ch input-ch delay-ch])]
                (condp = ch
                  select-ch (do
                              (actions/select-result val app)
                              (om/set-state! owner :results []))
                  input-ch  (om/set-state! owner :debounced true)
                  delay-ch  (when (om/get-state owner :debounced)
                              (actions/submit-search owner)
                              (om/set-state! owner :debounced false))))))))
    om/IRenderState
    (render-state [_ {:keys [results select-ch]}]
      (html
       [:div
        [:input
         {:ref       "search-term"
          :on-key-up #(handle-keyup % owner)}]
        (search-results results select-ch)]))))
