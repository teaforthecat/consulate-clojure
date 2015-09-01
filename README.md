# consulate-simple

A luminus project (+cljs) wih a simple consul http adapter

## Configuration
Drop an edn file next to the app.js file from the perspective of the browser.
For development, `resources/public/config.edn` with this content:

```clojure
{:env "dev"
 :consul-host-base-uri "http://localhost:8500/"}
```

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.


### Development

    lein figwheel

Connect to the webserver on port 3449

    open http://localhost:3449

Connect to the nrpel server on port 7002

### Emacs


```
M-x cider-connect
```

Enter "localhost" and "7002"
Then in the user namespace you can connect to the cljs-repl

```clojure
user> (cljs-repl)
```



## Running

To start a web server for the application, run:

    lein ring server

## Testing

    lein doo phantom test


[1]: https://github.com/technomancy/leiningen
