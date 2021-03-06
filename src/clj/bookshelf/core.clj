(ns bookshelf.core
  (:require [ring.util.response :refer [file-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.edn :as edn]
            [datomic.api :as d]
            [bookshelf.xml :as xml])
  (:import [java.net URLEncoder]))

(def uri "datomic:free://localhost:4334/bookshelf")
(def conn (d/connect uri))
(def base "https://www.goodreads.com")
(def key "mg5D9xctXLfojpfmQuBuQ")

(defn goodreads-api [endpoint query]
  (condp = endpoint
    :search (str "/search/index.xml?q=" query "&")
    :book   (str "/book/show/" query ".xml?")
    :author (str "/author/show/" query ".xml?")))

(defn api [endpoint query]
  (slurp (str base (goodreads-api endpoint query) "key=" key)))

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body    (pr-str data)})

(defn read-inputstream-edn [input]
  (edn/read
   {:eof nil}
   (java.io.PushbackReader.
    (java.io.InputStreamReader. input "UTF-8"))))

(defn parse-edn-body [handler]
  (fn [request]
    (handler (if-let [body (:body request)]
               (assoc request
                 :edn-body (read-inputstream-edn body))
               request))))

(defn encode-query [query]
  (URLEncoder/encode query "UTF-8"))

(defn book-details [data]
  (let [fields   {:book/id          [:id]
                  :book/title       [:title]
                  :book/author      [:authors :author :name]
                  :book/author-id   [:authors :author :id]
                  :book/description [:description]
                  :book/rating      [:average_rating]
                  :book/pages       [:num_pages]
                  :book/isbn        [:isbn13]
                  :book/year        [:work :original_publication_year]}
        details  (xml/get-fields data fields)
        defaults {:book/status "Unread"}]
    (merge details defaults)))

(defn similar-books [data]
  (let [books-data (xml/get-tag data :similar_books)
        fields     {:book/id        [:id]
                    :book/title     [:title_without_series]
                    :book/author    [:authors :author :name]
                    :book/author-id [:authors :author :id]}]
    (map #(xml/get-fields (:content %) fields) books-data)))

(defn book-summary [data]
  (let [result (xml/get-tag (:content data) :best_book)
        fields {:book/id     [:id]
                :book/title  [:title]
                :book/author [:author :name]}]
    (xml/get-fields result fields)))

(defn author-details [data]
  (let [fields {:author/id          [:id]
                :author/name        [:name]
                :author/about       [:about]}
        books  (map #(book-details (:content %)) (xml/get-tag data :books))]
    (assoc (xml/get-fields data fields) :author/books books)))

(defn books []
  (let [db (d/db conn)
        books
        (vec (map #(d/touch (d/entity db (first %)))
                  (d/q '[:find ?book
                         :where
                         [?book :book/id]]
                       db)))]
    (generate-response books)))

(defn get-book [id extraction-fn]
  (->> id
       (api :book)
       xml/parse-xml
       (xml/extract :book extraction-fn)))

(defn create-book [params]
  (let [details     (get-book (:book/id params) book-details)
        transaction (assoc details :db/id #db/id[:db.part/user])]
    (d/transact conn [transaction])
    (generate-response details)))

(defn update-book [id params]
  (let [db  (d/db conn)
        eid (ffirst
             (d/q '[:find ?book
                    :in $ ?id
                    :where
                    [?book :book/id ?id]]
                  db id))]
    (d/transact conn [(vec (flatten (concat [:db/add eid] params)))])
    (generate-response {:status :ok})))

(defn delete-book [id]
  (let [db  (d/db conn)
        eid (ffirst
             (d/q '[:find ?book
                    :in $ ?id
                    :where
                    [?book :book/id ?id]]
                  db id))]
    (d/transact conn [{:db/id     #db/id[:db.part/user]
                       :db/excise eid}])
    (generate-response {:status :ok})))

(defn search [query]
  (->> query
       encode-query
       (api :search)
       xml/parse-xml
       (xml/extract-books book-summary)
       generate-response))

(defn get-similar [id]
  (generate-response (get-book id similar-books)))

(defn get-author [id]
  (->> id
       (api :author)
       xml/parse-xml
       (xml/extract :author author-details)
       generate-response))

(defn index []
  (file-response "public/html/index.html" {:root "resources"}))

(defroutes routes
  (GET    "/"      [] (index))
  (GET    "/books" [] (books))
  (GET    "/search/:search"
          {params :params}
          (search (:search params)))
  (GET    "/books/:id"
          {params :params}
          (get-book (:id params)))
  (GET    "/books/:id/similar"
          {params :params}
          (get-similar (:id params)))
  (POST   "/books"
          {edn-body :edn-body}
          (create-book edn-body))
  (PUT    "/books/:id/update"
          {params :params edn-body :edn-body}
          (update-book (:id params) edn-body))
  (DELETE "/books/:id/delete"
          {params :params}
          (delete-book (:id params)))
  (GET    "/authors/:id"
          {params :params}
          (get-author (:id params)))
  (route/files "/" {:root "resources/public"}))

(def handler 
  (-> routes
      parse-edn-body))
