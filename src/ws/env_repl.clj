;; copyright (c) 2016-2018 world singles networks llc

(ns ws.env-repl
  "A Component that will start/stop REPL servers based on environment variables.

  REPL_PORT - if set, start a Socket REPL server on this port

  DEV_REPL_PORT - if set, start an optionally CIDER-enabled nREPL server on this port

  The latter requires that cider/cider-nrepl and refactor-nrepl are on the
  classpath. They are require'd at runtime.

  (env-repl/system) will return a Component; calling start on it will cause REPL
  servers to be started for each of the above environment variables, if set;
  calling stop on it will stop those REPL servers, if they are known to be
  started.

  (env-repl/system 'repl-name) will return a Component that will use repl-name
  as the name for the Socket REPL, if it starts one. The default is 'repl"
  (:require [clojure.core.server :as server]
            [com.stuartsierra.component :as component]))

(defn start-cider-nrepl
  "Given a port number, start an optionally CIDER-enabled nREPL server.
  Returns the server (so it can be stopped later)."
  [dev-repl-port]
  (try
    (println (str "Starting nREPL server on port " dev-repl-port "...\n"))
    (try
      (require '[nrepl.server :as nrepl])
      (catch Exception _
        (require '[clojure.tools.nrepl.server :as nrepl])))
    (try (require 'cider.nrepl) (catch Exception _))
    (try (require 'refactor-nrepl.middleware) (catch Exception _))
    (let [cider-mw (resolve 'cider.nrepl/cider-middleware)
          cider-mw (when cider-mw
                     (println "...including CIDER middleware...\n")
                     (map resolve (deref cider-mw)))
          refactor-mw (resolve 'refactor-nrepl.middleware/wrap-refactor)
          refactor-mw (when refactor-mw
                        (println "...including refactor-nrepl middleware...\n")
                        [refactor-mw])
          handler (apply (resolve 'nrepl/default-handler)
                         (concat cider-mw refactor-mw))
          server ((resolve 'nrepl/start-server)
                  :port dev-repl-port :handler handler)]
      (println (str "...nREPL server ready on port " dev-repl-port "\n"))
      server)
    (catch Exception e
      (println "Failed to start nREPL server\n" e))))

(defn stop-cider-nrepl
  "Given an nREPL server, stop it."
  [server]
  (try
    (println "Stopping nREPL server...\n")
    (try
      (require '[nrepl.server :as nrepl])
      (catch Exception _
        (require '[clojure.tools.nrepl.server :as nrepl])))
    ((resolve 'nrepl/stop-server) server)
    (println "...stopped\n")
    (catch Exception e
      (println "Failed to stop nREPL server\n" e))))

(defn start-socket-repl
  "Given a port number and a name, start a Socket REPL server.
  Returns the server (although it can be stopped by name later)."
  [repl-port repl-name]
  (println (str "Starting Socket REPL server " repl-name " on port " repl-port "...\n"))
  (let [server (server/start-server {:port repl-port
                                     :name repl-name
                                     :accept 'clojure.core.server/repl})]
    (println (str "...Socket REPL server " repl-name " ready on port " repl-port "\n"))
    server))

(defn stop-socket-repl
  "Given the name of a Socket REPL server, stop it."
  [repl-name]
  (println (str "Stopping Socket REPL server " repl-name "...\n"))
  (server/stop-server repl-name)
  (println "...stopped\n"))

(defrecord EnvironmentDrivenREPL [servers repl-name]
  component/Lifecycle

  (start [this]
    (let [dev-server (or (:dev servers)
                         (when-let [dev-repl-port (some-> (System/getenv "DEV_REPL_PORT")
                                                          Long/parseLong)]
                           (future ; because this is slow to startup...
                             (start-cider-nrepl dev-repl-port))))
          socket-svr (or (:socket servers)
                         (when-let [repl-port (some-> (System/getenv "REPL_PORT")
                                                      Long/parseLong)]
                           (start-socket-repl repl-port repl-name)))]
      (assoc this :servers {:dev dev-server :socket socket-svr})))

  (stop [this]
    (when-let [dev-server (:dev servers)]
      (stop-cider-nrepl @dev-server))
    (when (:socket servers)
      (stop-socket-repl repl-name))
    (assoc this :servers nil)))

(defn system
  "Create a new system. Optionally name the Socket REPL.
  Defaults to 'repl'."
  ([]
   (system 'repl))
  ([repl-name]
   (map->EnvironmentDrivenREPL {:repl-name (name repl-name)})))
