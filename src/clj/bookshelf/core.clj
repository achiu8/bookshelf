(ns bookshelf.core
  (:require [ring.util.response :refer [file-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes GET POST PUT]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.edn :as edn]
            [datomic.api :as d]))

(def uri "datomic:free://localhost:4334/bookshelf")
(def conn (d/connect uri))

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body    (pr-str data)})

(defn index []
  (file-response "public/html/index.html" {:root "resources"}))

(defn books []
  (let [db (d/db conn)
        books
        (vec (map #(d/touch (d/entity db (first %)))
                  (d/q '[:find ?book
                         :where
                         [?book :book/amazon-id]]
                       db)))]
    (generate-response books)))

(defn create-book [params]
  (let [id    (:book/amazon-id params)
        title (:book/title params)]
    (d/transact conn [{:db/id          #db/id[:db.part/user]
                       :book/amazon-id id
                       :book/title     title}])
    (generate-response {:status :ok})))

(defn update-book [id params]
  (let [db    (d/db conn)
        title (:book/title params)
        eid   (ffirst
               (d/q '[:find ?book
                      :in $ ?id
                      :where 
                      [?book :book/amazon-id ?id]]
                    db id))]
    (d/transact conn [[:db/add eid :book/title title]])
    (generate-response {:status :ok})))

(defroutes routes
  (GET  "/"      [] (index))
  (GET  "/books" [] (books))
  (POST "/books"
        {edn-body :edn-body}
        (create-book edn-body))
  (PUT  "/book/:id/update"
        {params :params edn-body :edn-body}
        (update-book (:id params) edn-body))
  (route/files "/" {:root "resources/public"}))

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

(def handler 
  (-> routes
      parse-edn-body))
