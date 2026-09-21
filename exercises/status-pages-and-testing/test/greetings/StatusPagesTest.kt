package greetings

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class StatusPagesTest {
  @Test
  fun `an unknown path gets the 404 page`() = testApplication {
    application {
      statusPages()
      routing { greetings() }
    }
    val response = client.get("/nowhere")
    assertEquals(HttpStatusCode.NotFound, response.status)
    assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
    assertContains(response.bodyAsText(), "<h1>Keep looking somewhere else</h1>")
  }

  @Test
  fun `your own exception gets its own page`() = testApplication {
    application {
      statusPages()
      routing { greetings() }
    }
    val response = client.get("/greet/ada/hello/3")
    assertEquals(HttpStatusCode.TooEarly, response.status)
    assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
    val body = response.bodyAsText()
    assertContains(body, "<h1>Come back later</h1>")
    assertContains(body, "<p>It is only 3 o'clock</p>")
  }

  @Test
  fun `a greeting after six is not rewritten`() = testApplication {
    application {
      statusPages()
      routing { greetings() }
    }
    val response = client.get("/greet/ada/hello/9")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello, ada, it's 9 o'clock", response.bodyAsText())
  }
}
