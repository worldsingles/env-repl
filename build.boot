(def project 'worldsingles/env-repl)
(def version "0.1.2")

(set-env! :resource-paths #{"src"}
          :dependencies   '[[org.clojure/clojure "1.8.0" :scope "provided"]
                            [com.stuartsierra/component "0.3.1"
                             :exclusions [org.clojure/clojure]]])

(task-options!
 pom {:project     project
      :version     version
      :description "Start/stop REPLs based on environment variables."
      :url         "https://bitbucket.org/wsnetworks/env-repl"
      :scm         {:url "https://bitbucket.org/wsnetworks/env-repl"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask deploy
  "Build and deploy (to your default repo)."
  []
  (comp (pom) (jar) (push)))

(defn cider-deps
  "Return current CIDER dependencies."
  []
  '[[cider/cider-nrepl "0.14.0"]
    [refactor-nrepl    "2.2.0"]])

(deftask with-cider
  "Add CIDER execution context."
  []
  (merge-env! :dependencies (cider-deps)))
