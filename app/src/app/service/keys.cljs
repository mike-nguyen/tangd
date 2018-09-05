(ns app.service.keys
  (:require
   [interop.core :as interop]
   [oops.core :as oops]
   [jose.core :as jose]
   [app.service.schema :as schema]
   [sqlite.core :as sqlite]
   [sqlite3]) )


(defn create-payload [jwk-es512 jwk-ecmr]
  (let [jwk-es512-pub (jose/jwk-pub jwk-es512)
        jwk-ecmr-pub (jose/jwk-pub jwk-ecmr) ]
    (jose/jwks->keys jwk-es512-pub jwk-ecmr-pub)))


(defn create-jws [payload & jwks]
  (let [n (count jwks)
        tmp "{\"protected\":{\"cty\":\"jwk-set+json\"}}"
        sig (jose/json-array (repeat n (jose/json-loads tmp)))
        jwks (jose/json-array jwks)]
    #_(println (jose/json-dumps (jose/jws-sig payload sig jwks)))
    (jose/jws-sig payload sig jwks)))


(defn clear-jws-table [db]
  (sqlite/on-cmd db :run "DELETE FROM jws; VACUUM;"))

(defn insert-jws [row]
  (print (.-jwk_id row)))

(defn cache-jws [db next-cmd]
  ((sqlite/on-cmd db :each schema/select-all-jwk)
   (fn [row]
     (insert-jws row)
     (next-cmd db))))

(defn insert-jwk [db])

(defn rotate-keys []
  (let [jwk-es512 (jose/jwk-gen "ES512")
        jwk-ecmr (jose/jwk-gen "ECMR")
        payload (create-payload jwk-es512 jwk-ecmr)
        jws (create-jws payload jwk-es512)
        db (sqlite/on-db)]
    ((clear-jws-table db)
     (fn [_](cache-jws db insert-jwk)))
    (jose/json-dumps jws)))
