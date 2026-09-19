# Handle, test, and measure the greetings

The exercises are in `src/greetings/StatusPages.kt` (the error pages),
`src/greetings/AppTest.kt` (the `appTest` helper the tests in
`test/greetings/GreetingTest.kt` run through) and `src/greetings/Metrics.kt`
(the Prometheus scrape). Check them with
`./kotlin test --include-module status-pages-and-testing` from `exercises/`.

## The property test

`GreetingTest.anyName` greets every name `Arb.string(minSize = 1)` generates.
Widen it to `Arb.string()` and run it: within a few dozen attempts `checkAll`
reports the empty string, because `/greet//bye` matches no route and the
server answers `404`. Then decide whether the server is wrong or the input
is: a name that cannot be a path segment is not a bug in `bye`, a name the
server rejects that a browser can send is. Narrow the generator
(`minSize = 1`, `Codepoint.alphanumeric()`) or fix the route until the test
says what you decided.

## Prometheus

The step that is not code: run the module in the project of lesson 1, put
this in `prometheus.yml`

```yaml
scrape_configs:
  - job_name: greetings
    scrape_interval: 5s
    static_configs:
      - targets: ["host.docker.internal:8080"]
```

and start Prometheus with
`docker run -p 9090:9090 -v ./prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus`.
Send a few greetings, open `http://localhost:9090` and graph
`rate(ktor_http_server_requests_seconds_count[1m])` by `route`.
