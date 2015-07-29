# consulate-simple

A luminus project (+cljs) wih a simple consul http adapter

## Configuration
Drop an edn file next to the app.js file from the perspective of the browser.
For development, `resources/public/config.edn`

```clojure
{:env "dev"
 :consul-host-base-uri "http://localhost:8500/"}
```
## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

### Development

    lein figwheel


### Emacs

```
M-x cider-connect
```

```clojure
user> (cljs-repl)
```

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2015 ct
