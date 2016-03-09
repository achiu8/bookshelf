(ns bookshelf.xml
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]))

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

(defn get-fields [data fields]
  (reduce #(assoc %1 %2 (get-tag-path (%2 %1) data)) fields (keys fields)))

(defn extract-book [extraction-fn parsed]
  (-> parsed
      (get-tag :book)
      extraction-fn))

(defn extract-books [extraction-fn parsed]
  (-> parsed
      second :content
      (get-tag :results)
      (->> (map extraction-fn))))

