(ns consulate-simple.config
  (:require [ajax.core :refer [GET]]))

(def config (atom {:root-path "/"
                   :env "dev"
                   :consul-host-base-uri "/"}))
(defn get-config []
  (GET "config.edn"
      :handler  #(swap! config (partial merge %))
      :error-handler (fn [] (.log js/console "using default configuration"))))
