(ns app.controller.rec
  (:require
   [registrar.core :as registrar]))

(defn rec-response [[kid name] res-spec]
  #js {:kid kid :name name})

(registrar/reg-evt :rec :restify
                   rec-response
                   [[:params :kid] [:body :name]] {:status :CREATED})
