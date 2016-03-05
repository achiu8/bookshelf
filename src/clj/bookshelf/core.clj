(ns bookshelf.core
  (:require [ring.util.response :refer [file-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.edn :as edn]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [datomic.api :as d])
  (:import [java.net URLEncoder]))

(def uri "datomic:free://localhost:4334/bookshelf")
(def conn (d/connect uri))
(def base "https://www.goodreads.com")
(def key "mg5D9xctXLfojpfmQuBuQ")

(defn goodreads-api [endpoint query]
  (condp = endpoint
    :search (str "/search/index.xml?q=" query "&")
    :book   (str "/book/show/" query ".xml?")))

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

(defn parse-xml [xml]
  (->> (java.io.ByteArrayInputStream. (.getBytes xml))
       xml/parse
       zip/xml-zip
       first
       :content))

(defn get-tag [data tag]
  (->> data
       (filter #(= (:tag %) tag))
       first
       :content))

(defn get-tag-path [path data]
  (first (reduce get-tag data path)))

(defn book-details [data]
  (let [paths {:book/id          [:id]
               :book/title       [:title]
               :book/author      [:authors :author :name]
               :book/description [:description]
               :book/rating      [:average_rating]
               :book/pages       [:num_pages]
               :book/isbn        [:isbn13]
               :book/year        [:work :original_publication_year]}
        fields (keys paths)]
    (reduce #(assoc %1 %2 (get-tag-path (%2 %1) data)) paths fields)))

(defn book-summary [data]
  (let [result (get-tag (:content data) :best_book)]
    {:id     (get-tag-path [:id] result)
     :title  (get-tag-path [:title] result)
     :author (get-tag-path [:author :name] result)}))

(defn extract-book [parsed]
  (-> parsed
      (get-tag :book)
      book-details))

(defn extract-books [parsed]
  (-> parsed
      second :content
      (get-tag :results)
      (->> (map book-summary))))

(defn index []
  (file-response "public/html/index.html" {:root "resources"}))

(defn books []
  (let [db (d/db conn)
        books
        (vec (map #(d/touch (d/entity db (first %)))
                  (d/q '[:find ?book
                         :where
                         [?book :book/id]]
                       db)))]
    (generate-response books)))

(defn get-book [id]
  (->> id
       (api :book)
       parse-xml
       extract-book))

(defn create-book [params]
  (let [book-details (get-book (:book/id params))
        transaction  (assoc book-details :db/id #db/id[:db.part/user])]
    (d/transact conn [transaction])
    (generate-response book-details)))

(defn update-book [id params]
  (let [db    (d/db conn)
        title (:book/title params)
        eid   (ffirst
               (d/q '[:find ?book
                      :in $ ?id
                      :where 
                      [?book :book/id ?id]]
                    db id))]
    (d/transact conn [[:db/add eid :book/title title]])
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
       parse-xml
       extract-books
       generate-response))

(defroutes routes
  (GET    "/"      [] (index))
  (GET    "/books" [] (books))
  (GET    "/search/:search"
          {params :params}
          (search (:search params)))
  (GET    "/books/:id"
          {params :params}
          (get-book (:id params)))
  (POST   "/books"
          {edn-body :edn-body}
          (create-book edn-body))
  (PUT    "/books/:id/update"
          {params :params edn-body :edn-body}
          (update-book (:id params) edn-body))
  (DELETE "/books/:id/delete"
          {params :params}
          (delete-book (:id params)))
  (route/files "/" {:root "resources/public"}))

(def handler 
  (-> routes
      parse-edn-body))
