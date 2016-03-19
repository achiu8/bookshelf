(ns bookshelf.utils
  (:require [om.core :as om :include-macros true]))

(defn throttle [f owner]
  (fn [& args]
    (when-not (om/get-state owner :throttled)
      (apply f args)
      (om/set-state! owner :throttled true)
      (js/setTimeout #(om/set-state! owner :throttled false) 250))))

(defn highlight [search-term title]
  (cond (empty? search-term) title
        (empty? title) '()
        (= (clojure.string/lower-case (first search-term))
           (clojure.string/lower-case (first title)))
        (cons [:b (str (first title))]
              (highlight (rest search-term) (rest title)))
        :else (cons (first title) (highlight search-term (rest title)))))
