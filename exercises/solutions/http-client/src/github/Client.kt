package github

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun client(): HttpClient = HttpClient(CIO) {
  install(ContentNegotiation) {
    json(Json { ignoreUnknownKeys = true })
  }
  install(Resources)
  defaultRequest { url("https://api.github.com") }
  install(HttpRequestRetry) {
    retryOnServerErrors(maxRetries = 3)
    exponentialDelay()
  }
}
