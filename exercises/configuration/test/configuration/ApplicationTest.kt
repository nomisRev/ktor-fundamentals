package configuration

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApplicationTest {
  @Test
  fun `application yaml holds the port and the secret`() {
    val config = ApplicationConfig("application.yaml")
    assertEquals("8080", config.propertyOrNull("ktor.deployment.port")?.getString(), "no ktor.deployment.port")
    assertNotNull(config.propertyOrNull("jwt.secret"), "no jwt.secret")
    assertEquals(listOf("configuration.ApplicationKt.module"), config.property("ktor.application.modules").getList())
  }

  @Test
  fun `settings reads the configuration`() = testApplication {
    environment {
      config = MapApplicationConfig(
        "ktor.deployment.port" to "9090",
        "jwt.secret" to "s3cret",
      )
    }
    application {
      assertEquals(Settings(port = 9090, jwtSecret = "s3cret"), settings())
      module()
    }
    assertEquals("Greeting service on port 9090", client.get("/").bodyAsText())
  }

  @Test
  fun `the module is loaded from application yaml`() = testApplication {
    environment {
      config = ApplicationConfig("application.yaml")
    }
    assertEquals("Greeting service on port 8080", client.get("/").bodyAsText())
  }
}
