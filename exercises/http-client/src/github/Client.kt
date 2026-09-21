package github

import io.ktor.client.HttpClient

/**
 * Exercise 1 / 4: one client, three plug-ins.
 *
 * `client` builds an `HttpClient(CIO)` with `ContentNegotiation`
 * (`json(Json { ignoreUnknownKeys = true })`), a
 * `defaultRequest { url("https://api.github.com") }` so that every request
 * only names its path, and `HttpRequestRetry`
 * (`retryOnServerErrors(maxRetries = 3)` with `exponentialDelay()`).
 */
fun client(): HttpClient = TODO()
