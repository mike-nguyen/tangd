(ns app.controller.keys
  (:require
   [async.restify.core :as restify]
   [sqlite.core :as sqlite]
   [async.sqlite.core :as sqlite* :refer-macros [transaction]]
   [async.core :as async :refer-macros [<?* <?_ <? go-try go  <!]]
   [app.service.keys :as keys]))


(defn insert-thp-jwk [db jwk]
  (fn [result]
    (let [jwk-id (.-lastID result)]
      (go-try
       (->> ((keys/insert-thp db jwk) result)
            (mapv #(go-try ((keys/insert-thp-jwk db jwk-id) (<? %)))))))))


(defn rotate-keys* [db init-vals]
  (let [[es512 ecmr payload jws] init-vals]
    (sqlite*/transaction
     db [(fn [_]
           (keys/cache-defaults jws)
           {:status :CREATED})]
     (go-try
      (-> (keys/insert-jwk db ecmr)
          (<?) ((insert-thp-jwk db ecmr))
          (<?_ (keys/insert-jwk db es512))
          (<?) ((insert-thp-jwk db es512))
          (<?_ (keys/drop-jws-table db))
          (<?_ (keys/create-jws-table db))
          (<?_ (keys/create-jws-jwk-index db))
          (<?_ (keys/select-all-jwk db))
          (<?* number? (keys/insert-jws db payload es512)))))))


(defn rotate-keys []
  (let [init-vals (keys/rotate-keys)
        [es512 ecmr payload jws] init-vals
        sqlite-db (sqlite/on-db)]
    (rotate-keys* sqlite-db init-vals)))


(restify/reg-http-request-handler
 :keys
 (fn [context]
   (go
     (->> (rotate-keys)
          (<!) (restify/check-error-result)
          (restify/http-response :keys)))))

;; (def handler (restify/handle-route restify-route-event))
