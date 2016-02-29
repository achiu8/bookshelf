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

(defn index []
  (file-response "public/html/index.html" {:root "resources"}))

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body    (pr-str data)})

(defn create-class [params]
  (let [id    (:class/id params)
        title (:class/title params)]
    (d/transact conn [{:db/id       #db/id[:db.part/user]
                       :class/id    id
                       :class/title title}])
    (generate-response {:status :ok})))

(defn update-class [id params]
  (let [db    (d/db conn)
        title (:class/title params)
        eid   (ffirst
               (d/q '[:find ?class
                      :in $ ?id
                      :where 
                      [?class :class/id ?id]]
                    db id))]
    (d/transact conn [[:db/add eid :class/title title]])
    (generate-response {:status :ok})))

(defn classes []
  (let [db (d/db conn)
        classes
        (vec (map #(d/touch (d/entity db (first %)))
                  (d/q '[:find ?class
                         :where
                         [?class :class/id]]
                       db)))]
    (generate-response classes)))

(defroutes routes
  (GET  "/"        [] (index))
  (GET  "/classes" [] (classes))
  (POST "/classes"
        {edn-body :edn-body}
        (create-class edn-body))
  (PUT  "/class/:id/update"
        {params :params edn-body :edn-body}
        (update-class (:id params) edn-body))
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
