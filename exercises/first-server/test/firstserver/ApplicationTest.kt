package firstserver

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
  @Test
  fun `hello responds with text`() = testApplication {
    application { module() }
    val response = client.get("/hello")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello, world!", response.bodyAsText())
    assertEquals(ContentType.Text.Plain, response.contentType()?.withoutParameters())
  }

  @Test
  fun `post hello returns an explicit status`() = testApplication {
    application { module() }
    val response = client.post("/hello")
    assertEquals(HttpStatusCode.Created, response.status)
    assertEquals("Created!", response.bodyAsText())
  }

  @Test
  fun `an unknown path is a 404`() = testApplication {
    application { module() }
    assertEquals(HttpStatusCode.NotFound, client.get("/nowhere").status)
  }
}
