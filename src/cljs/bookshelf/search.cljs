(ns bookshelf.search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<! alts! put! chan timeout]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [bookshelf.actions :as actions]
            [bookshelf.utils :as utils]))

(defn select-up [selected]
  (max (dec selected) 0))

(defn select-down [selected results]
  (min (inc selected) (dec (count results))))

(defn handle-select [_ selected owner]
  (when selected
    (let [result (nth (om/get-state owner :results) selected)]
      (om/set-state! owner :results [])
      (put! (om/get-shared owner :select-ch) result))))

(defn handle-keydown [e owner]
  (let [results  (om/get-state owner :results)
        selected (om/get-state owner :selected)
        input-ch (om/get-state owner :input-ch)]
    (condp = (.-keyCode e)
      38 (om/update-state! owner :selected (fnil select-up 0))
      40 (om/update-state! owner :selected (fnil #(select-down % results) -1))
      13 (handle-select nil selected owner)
      (do (om/set-state! owner :selected nil)
          ((utils/throttle actions/submit-search owner) owner)
          (put! input-ch (.. e -target -value))))))

(defn handle-results [results owner]
  (om/set-state! owner
                 :results
                 (if (empty? (.-value (om/get-node owner "search-term")))
                   []
                   results)))

(defn search-result [result i owner]
  (html
   [:div.clickable
    {:style         {:background-color (when (= i (om/get-state owner :selected)) "#f0f0f0")}
     :on-click      #(handle-select % i owner)
     :on-mouse-over #(om/set-state! owner :selected i)}
    (utils/highlight (seq (.-value (om/get-node owner "search-term")))
                     (seq (:book/title result)))]))

(defn search-results [results owner]
  (html
   [:div
    (map-indexed #(search-result %2 %1 owner) results)]))

(defn search [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:results    []
       :selected   0
       :throttled  false
       :debounced  false
       :input-ch   (chan)
       :results-ch (chan)})
    om/IWillMount
    (will-mount [_]
      (go (while true
            (let [input-ch   (om/get-state owner :input-ch)
                  results-ch (om/get-state owner :results-ch)
                  delay-ch   (timeout 500)
                  [val ch] (alts! [input-ch results-ch delay-ch])]
              (condp = ch
                input-ch   (om/set-state! owner :debounced true)
                results-ch (handle-results val owner)
                delay-ch   (when (om/get-state owner :debounced)
                             (actions/submit-search owner)
                             (om/set-state! owner :debounced false)))))))
    om/IRenderState
    (render-state [_ {:keys [results selected]}]
      (html
       [:div.search
        [:input
         {:ref         "search-term"
          :on-key-down #(handle-keydown % owner)}]
        (search-results results owner)]))))
