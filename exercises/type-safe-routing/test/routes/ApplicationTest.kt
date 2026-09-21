package routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ApplicationTest {
  @Test
  fun `bye is a text response`() = testApplication {
    application { module() }
    val response = client.get("/greet/ada/bye")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Bye, ada", response.bodyAsText())
  }

  @Test
  fun `hello renders HTML`() = testApplication {
    application { module() }
    val response = client.get("/greet/ada/hello/9")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
    val body = response.bodyAsText()
    assertContains(body, "<title>World Greeting Service</title>")
    assertContains(body, "<h1>Hello, ada, it's 9 o'clock</h1>")
  }

  @Test
  fun `lang picks the greeting`() = testApplication {
    application { module() }
    assertContains(client.get("/greet/linus/hello/17?lang=nl").bodyAsText(), "<h1>Hallo, linus, it's 17 o'clock</h1>")
    assertContains(client.get("/greet/linus/hello/17").bodyAsText(), "<h1>Hello, linus, it's 17 o'clock</h1>")
  }

  @Test
  fun `the hour must be a number`() = testApplication {
    application { module() }
    assertEquals(HttpStatusCode.BadRequest, client.get("/greet/ada/hello/nine").status)
  }

  @Test
  fun `the hour must be on the clock`() = testApplication {
    application { module() }
    assertEquals(HttpStatusCode.BadRequest, client.get("/greet/ada/hello/25").status)
    assertEquals(HttpStatusCode.BadRequest, client.get("/greet/ada/hello/-1").status)
    assertEquals(HttpStatusCode.OK, client.get("/greet/ada/hello/0").status)
    assertEquals(HttpStatusCode.OK, client.get("/greet/ada/hello/23").status)
  }

  @Test
  fun `the DSL escapes a name with markup`() = testApplication {
    application { module() }
    val body = client.get("/greet/${"<b>grace</b>".encodeURLPathPart()}/hello/9").bodyAsText()
    assertContains(body, "Hello, &lt;b&gt;grace&lt;/b&gt;, it's 9 o'clock")
    assertFalse("<b>grace</b>" in body, "the name was not escaped")
  }
}
