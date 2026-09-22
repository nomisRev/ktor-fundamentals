package configuration

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
  @Test
  fun `application yaml holds the host and the port`() {
    val config = ApplicationConfig("application.yaml")
    assertEquals("0.0.0.0", config.propertyOrNull("server.host")?.getString(), "no server.host")
    assertEquals("8080", config.propertyOrNull("server.port")?.getString(), "no server.port")
  }

  @Test
  fun `loadConfig deserialises the file`() {
    assertEquals(Config(Server(host = "0.0.0.0", port = 8080)), loadConfig())
  }

  @Test
  fun `the module answers from the loaded values`() = testApplication {
    application {
      module(Config(Server(host = "localhost", port = 9090)))
    }
    assertEquals("Greeting service on localhost:9090", client.get("/").bodyAsText())
  }
}
