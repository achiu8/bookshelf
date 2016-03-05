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
  (endpoint
   {:search (str "/search/index.xml?q=" query "&")
    :book   (str "/book/show/" query ".xml?")}))

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

(defn get-tag [tag results]
  (->> results
       (filter #(= (:tag %) tag))
       first
       :content))

(defn encode-query [query]
  (URLEncoder/encode query "UTF-8"))

(defn parse-xml [xml]
  (zip/xml-zip (xml/parse (java.io.ByteArrayInputStream. (.getBytes xml)))))

(defn extract-books [parsed]
  (->> parsed
       first :content second :content
       (get-tag :results)
       (map (fn [data]
              (let [result (get-tag :best_book (:content data))]
                {:id     (first (get-tag :id result))
                 :title  (first (get-tag :title result))
                 :author (first (:content (second (get-tag :author result))))})))))

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

(defn create-book [params]
  (let [id     (:book/id params)
        title  (:book/title params)
        author (:book/author params)]
    (d/transact conn [{:db/id       #db/id[:db.part/user]
                       :book/id     id
                       :book/title  title
                       :book/author author}])
    (generate-response {:status :ok})))

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

(defn get-book [id]
  (->> id
       (api :book)
       parse-xml
       println))

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
