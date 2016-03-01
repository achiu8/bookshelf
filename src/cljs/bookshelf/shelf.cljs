(ns bookshelf.shelf
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [bookshelf.xhr :as xhr]
              [bookshelf.editable :as editable]))

(defn on-edit [id title]
  (xhr/xhr
   {:method      :put
    :url         (str "book/" id "/update")
    :data        {:book/title title}
    :on-complete (fn [res]
                   (println "server response:" res))}))

(defn add-book [id title]
  (xhr/xhr
   {:method      :post
    :url         "books"
    :data        {:book/id id :book/title title}
    :on-complete (fn [res]
                   (println "server response:" res))}))

(defn create-book [books owner]
  (let [book-id-el   (om/get-node owner "book-id")
        book-id      (.-value book-id-el)
        book-name-el (om/get-node owner "book-name")
        book-name    (.-value book-name-el)
        new-book     {:book/id book-id :book/title book-name}]
    (om/transact! books [] #(conj % new-book))
    (set! (.-value book-id-el) "")
    (set! (.-value book-name-el) "")
    (add-book book-id book-name)))

(defn book [book]
  (dom/tr
   nil
   (dom/td
    nil
    (dom/a
     #js {:href (str "https://www.goodreads.com/book/show/"
                     (:book/id book))}
     (:book/title book)))
   (dom/td nil (:book/author book))
   (dom/td nil (:book/genre book))))

(defn books [books]
  (apply dom/table
         nil
         (dom/tr
          nil
          (dom/th nil "Title")
          (dom/th nil "Author")
          (dom/th nil "Genre"))
         (map book books)))

(defn throttle [f owner]
  (fn [& args]
    (when-not (om/get-state owner :throttled)
      (apply f args)
      (om/set-state! owner :throttled true)
      (js/setTimeout #(om/set-state! owner :throttled false) 500))))

(defn search [books owner]
  (let [search-term-input (om/get-node owner "search-term")
        search-term       (.-value search-term-input)]
    (when-not (zero? (count search-term))
      (xhr/xhr
       {:method :get
        :url (str "search/" search-term)
        :on-complete #(om/set-state! owner :results %)}))))

(defn search-result [result owner]
  (dom/div
   nil
   (:title result)))

(defn search-results [results owner]
  (apply dom/div
         nil
         (map search-result results)))

(defn shelf [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:results []})
    om/IWillMount
    (will-mount [_]
      (xhr/xhr
       {:method      :get
        :url         "books"
        :on-complete #(om/transact! app :books (fn [_] %))}))
    om/IRenderState
    (render-state [_ {:keys [results]}]
      (dom/div
       #js {:id "books"}
       (dom/h2 nil "Books")
       (books (:books app))
       (dom/div
        nil
        (dom/input
         #js {:ref "search-term"
              :onKeyUp #((throttle search owner) (:books app) owner)})
        (dom/button
         #js {:onClick #(search (:books app) owner)}
         "Search"))
       (search-results results owner)))))

