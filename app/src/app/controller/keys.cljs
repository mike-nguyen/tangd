(ns app.controller.keys
  (:require
   [app.service.keys :as keys]
   [app.coop :as coop]))

(coop/reg-event-fx
 :rotate-keys
 keys/rotate-keys)
