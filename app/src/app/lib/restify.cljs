(ns app.lib.restify
  (:require
   [restify-errors :as errors]
   [app.lib.interop :as interop]
   [app.registrar :as registrar]
   [cljs.core]
   [app.config :as config]
   [app.lib.const* :as const]
   [oops.core :as oops]))

(defn extractor- [req path]
  (let [content-type (oops/ocall req :contentType)
        [first & rest] path
        is-transit? (re-matches #"(?i)application/.*(json|msgpack)" content-type)
        extractor (if is-transit? get-in #(oops/oget+ %1 %2))
        o (oops/oget+ req first)
        extractor (partial extractor o)]
    (extractor rest)))

(defn extract-request [paths _ [req]]
  (let [extractor (partial extractor- req)
        data (mapv extractor paths)
        _ (println "extracted: " data)]
    {:data data}))

(defn apply-defaults [_ res-spec _]
  {:res-spec (merge config/response-defaults res-spec)})

(defn apply-status [_ res-spec _]
  (let [{:keys [status]}  res-spec]
    {:res-spec (assoc res-spec :status (status const/http-status))}))

(defn check-http-error [data] (cljs.core/instance? errors/HttpError data))

(defn send- [res next* status data headers next?]
  (let [http-error? (check-http-error data)
        next? (if http-error? data next?)
        _ (println "sending: " data)]
    (when-not http-error? (oops/ocall res :send status data headers)) (next* next?)))

(defn respond [data res-spec dispatch-data]
  (let [[req res next*] dispatch-data
        {:keys [status headers next?]} res-spec]
    (send- res next* status data headers next?)))

(defn wrap-skip-if-error [f data res-spec dispatch-data]
  (when-not (check-http-error data) (f data res-spec dispatch-data)))

(defn wrap-fv [w fv] (mapv #(partial w %) fv))

(def pre-callback-fv (wrap-fv wrap-skip-if-error [extract-request]))
(def post-callback-fv (wrap-fv wrap-skip-if-error [apply-defaults apply-status]))

(registrar/reg-fx :restify respond pre-callback-fv post-callback-fv)
