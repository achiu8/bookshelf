(ns bookshelf.utils
  (:require [om.core :as om :include-macros true]))

(defn throttle [f owner]
  (fn [& args]
    (when-not (om/get-state owner :throttled)
      (apply f args)
      (om/set-state! owner :throttled true)
      (js/setTimeout #(om/set-state! owner :throttled false) 250))))
