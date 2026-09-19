package greetings

import io.ktor.server.application.Application
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/** The registry the server and the scrape share. */
val prometheus: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

/**
 * Exercise 4 / 5: expose the numbers.
 *
 * `metrics` installs `MicrometerMetrics` with `registry = registry` and
 * registers `get("/metrics")`, which responds with `registry.scrape()`: one
 * `ktor_http_server_requests_seconds` line per route and status.
 *
 * Exercise 5 / 5 has no code: scrape `/metrics` with a local Prometheus and
 * graph the requests per route; see README.md.
 */
fun Application.metrics(registry: PrometheusMeterRegistry = prometheus): Unit = TODO()
