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

(defn get-tag [tag results]
  (filter #(= (:tag %) tag) results))

(defn extract-books [parsed]
  (->> parsed
       first :content second :content
       (get-tag :results)
       first :content
       (map (fn [data]
              (let [result (:content (first (get-tag :best_book (:content data))))]
                {:id     (first (:content (first (get-tag :id result))))
                 :title  (first (:content (first (get-tag :title result))))
                 :author (first (:content (second (:content (first (get-tag :author result))))))})))))

(defn search [search]
  (let [query (URLEncoder/encode search "UTF-8")
        results (slurp
                 (str "https://www.goodreads.com/search/index.xml?key=mg5D9xctXLfojpfmQuBuQ&q="
                      query))
        parsed (zip/xml-zip (xml/parse (java.io.ByteArrayInputStream. (.getBytes results))))]
    (generate-response (extract-books parsed))))

(defroutes routes
  (GET    "/"      [] (index))
  (GET    "/books" [] (books))
  (GET    "/search/:search"
          {params :params}
          (search (:search params)))
  (POST   "/books"
          {edn-body :edn-body}
          (create-book edn-body))
  (PUT    "/book/:id/update"
          {params :params edn-body :edn-body}
          (update-book (:id params) edn-body))
  (DELETE "/book/:id/delete"
          {params :params}
          (delete-book (:id params)))
  (route/files "/" {:root "resources/public"}))

(def handler 
  (-> routes
      parse-edn-body))
