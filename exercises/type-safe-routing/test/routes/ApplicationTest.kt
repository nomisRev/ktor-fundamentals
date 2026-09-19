package routes

import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.resources.href
import io.ktor.resources.serialization.ResourcesFormat
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ApplicationTest {
  @Test
  fun `the resources describe the paths`() {
    val format = ResourcesFormat()
    assertEquals("/greet/ada?lang=en", href(format, Greeting(name = "ada")))
    assertEquals("/greet/ada/bye?lang=en", href(format, Greeting.Bye(parent = Greeting(name = "ada"))))
    assertEquals(
      "/greet/ada/hello/9?lang=nl",
      href(format, Greeting.Hello(parent = Greeting(name = "ada", lang = "nl"), hour = 9)),
    )
  }

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
    val client = createClient { install(Resources) }
    val response = client.get(Greeting.Hello(parent = Greeting(name = "linus", lang = "nl"), hour = 17))
    assertContains(response.bodyAsText(), "<h1>Hallo, linus, it's 17 o'clock</h1>")
  }

  @Test
  fun `the hour must be a number`() = testApplication {
    application { module() }
    assertEquals(HttpStatusCode.BadRequest, client.get("/greet/ada/hello/nine").status)
  }

  @Test
  fun `the DSL escapes a name with markup`() = testApplication {
    application { module() }
    val client = createClient { install(Resources) }
    val body = client.get(Greeting.Hello(parent = Greeting(name = "<b>grace</b>"), hour = 9)).bodyAsText()
    assertContains(body, "Hello, &lt;b&gt;grace&lt;/b&gt;, it's 9 o'clock")
    assertFalse("<b>grace</b>" in body, "the name was not escaped")
  }
}
