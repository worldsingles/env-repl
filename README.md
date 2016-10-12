# env-repl

A Component that will start/stop REPL servers based on environment variables.

## Usage

Leiningen / Boot Dependency:

``` clojure
[worldsingles/env-repl "0.1.0"]
```

Start the REPL with CIDER dependencies, and environment variables defining the PORTs on which you want to run REPL:

``` shell
DEV_REPL_PORT=6100 REPL_PORT=7100 boot -d cider/cider-nrepl -d refactor-nrepl repl
```

Build and start the Component in the REPL:

``` clojure
(require '[ws.env-repl :as env-repl]
         '[com.stuartsierra.component :as component])

(def system (env-repl/system 'my-repl))
(alter-var-root #'system component/start)
```

If you do not plan to start a DEV REPL, you do not need the CIDER dependencies. You can always start a Socket REPL:

``` clojure
(defn -main [& args]
  (component/start (component/system-map :repl (env-repl/system))))
```

``` shell
REPL_PORT=54321 java -jar path/to/my-uberjar-0.1.0.jar
```

## License

Copyright © 2016 World Singles llc

Distributed under the Eclipse Public License version 1.0.
