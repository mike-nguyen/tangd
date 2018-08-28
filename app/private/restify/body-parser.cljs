(ns restify.body-parser
  (:require [restify]
            [restify-errors :as errors]
            [restify.transit-parser :as transit-parser]
            [restify.jwk-parser :as jwk-parser]
            ;; [app.lib.fast-json-parser :as json-parser]
            [oops.core :as oops]))

(defn parse-body- [parsers]
  (fn [req res next]
    (let [method (oops/oget req :method)
          is-chunked (oops/ocall req :isChunked)
          length (oops/ocall req :contentLength)]
      (cond
        (oops/oget req "?_parsedBody") (next)
        (= method "HEAD") (next)
        (= method "GET") (next)
        (and (= length 0) (not is-chunked)) (next)
        :else
        (let [content-type (oops/ocall req :contentType)
              content-parser
              (case content-type
                "application/json" (:json-parser parsers)
                "application/transit+json" (:transit-parser parsers)
                "application/jwk+json" (:jwk-parser parsers)
                ;; "application/transit+msgpack" (:transit-parser parsers)
                "application/x-www-form-urlencoded" (:form-parser parsers)
                "multipart/form-data" (:multipart-parser parsers)
                (when (re-matches #"(?i)application/.*\+json" content-type)
                  (:json-parser parsers)))]
          (oops/oset! req "!_parsedBody" true)
          (if content-parser
            (content-parser req res next)
            (next (errors/UnsupportedMediaTypeError. content-type))))))))

(defn body-parser [& [options]]
  (let [opts (or options #js {})
        _ (oops/oset! opts "!bodyReader" true)
        body-reader (oops/ocall restify "plugins.bodyReader" opts)
        parsers {:form-parser (aget (oops/ocall restify "plugins.urlEncodedBodyParser" opts) 0)
                 :multipart-parser (oops/ocall restify "plugins.multipartBodyParser" opts)
                 :json-parser (aget (oops/ocall restify "plugins.jsonBodyParser" opts) 0)
                 :transit-parser transit-parser/transit-parser
                 :jwk-parser jwk-parser/jwk-parser}]
    #js [body-reader (parse-body- parsers)]))

