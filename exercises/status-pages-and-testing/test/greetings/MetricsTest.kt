package greetings

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MetricsTest {
  @Test
  fun `metrics count the requests per route`() = testApplication {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    application {
      metrics(registry)
      routing {
        get("/ping") { call.respondText("pong") }
      }
    }
    repeat(3) { assertEquals("pong", client.get("/ping").bodyAsText()) }
    val response = client.get("/metrics")
    assertEquals(HttpStatusCode.OK, response.status)
    val scrape = response.bodyAsText()
    assertContains(scrape, "ktor_http_server_requests_seconds")
    assertContains(scrape, """route="/ping"""")
    assertContains(scrape, """status="200"""")
  }

  @Test
  fun `the scrape is plain text for Prometheus`() = testApplication {
    application {
      metrics(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
    }
    val scrape = client.get("/metrics").bodyAsText()
    assertContains(scrape, "# TYPE jvm_memory_used_bytes gauge")
  }
}
