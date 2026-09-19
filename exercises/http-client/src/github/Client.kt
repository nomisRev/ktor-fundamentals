package github

import io.ktor.client.HttpClient

/**
 * Exercise 2 / 5: one client, four plug-ins.
 *
 * `client` builds an `HttpClient(CIO)` with `ContentNegotiation`
 * (`json(Json { ignoreUnknownKeys = true })`), `Resources`, a
 * `defaultRequest { url("https://api.github.com") }` and `HttpRequestRetry`
 * (`retryOnServerErrors(maxRetries = 3)` with `exponentialDelay()`).
 */
fun client(): HttpClient = TODO()
