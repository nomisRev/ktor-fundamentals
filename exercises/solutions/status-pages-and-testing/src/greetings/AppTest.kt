package greetings

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication

fun appTest(test: suspend (HttpClient) -> Unit): Unit = testApplication {
  application { module() }
  val client = createClient {
    install(ContentNegotiation) { json() }
    install(Resources)
  }
  test(client)
}
