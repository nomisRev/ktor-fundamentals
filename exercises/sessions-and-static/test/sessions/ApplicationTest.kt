package sessions

import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.setCookie
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApplicationTest {
  @Test
  fun `hello counts the visits`() = testApplication {
    application { module() }
    val client = createClient { install(HttpCookies) }
    assertContains(client.get("/greet/ada/hello").bodyAsText(), "<p>Visit 1</p>")
    assertContains(client.get("/greet/ada/hello").bodyAsText(), "<p>Visit 2</p>")
    assertEquals("Bye, ada", client.get("/greet/ada/bye").bodyAsText())
    assertContains(client.get("/greet/ada/hello").bodyAsText(), "<p>Visit 1</p>")
  }

  @Test
  fun `hello renders HTML`() = testApplication {
    application { module() }
    val response = client.get("/greet/linus/hello")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
    assertContains(response.bodyAsText(), "<h1>Hello, linus</h1>")
  }

  @Test
  fun `the cookie is signed`() = testApplication {
    application { module() }
    val cookie = client.get("/greet/ada/hello").setCookie().singleOrNull { it.name == "visits" }
    assertNotNull(cookie, "expected a Set-Cookie header for `visits`")
    assertEquals("/", cookie.path)
    assertContains(cookie.value, "/", message = "a signed value is `payload/signature`")
    val forged = cookie.value.substringBeforeLast("/") + "/0000"
    val response = client.get("/greet/ada/hello") {
      header(HttpHeaders.Cookie, "visits=${forged.encodeURLParameter()}")
    }
    assertContains(response.bodyAsText(), "<p>Visit 1</p>")
  }

  @Test
  fun `the assets are served`() = testApplication {
    application { module() }
    val css = client.get("/assets/style.css")
    assertEquals(HttpStatusCode.OK, css.status)
    assertEquals(ContentType.Text.CSS, css.contentType()?.withoutParameters())
    val logo = client.get("/assets/logo.svg")
    assertEquals(HttpStatusCode.OK, logo.status)
    assertEquals(ContentType.Image.SVG, logo.contentType()?.withoutParameters())
    assertEquals(HttpStatusCode.NotFound, client.get("/assets/missing.css").status)
  }

  @Test
  fun `the page links the assets`() = testApplication {
    application { module() }
    val body = client.get("/greet/grace/hello").bodyAsText()
    assertContains(body, """href="/assets/style.css"""")
    assertContains(body, """src="/assets/logo.svg"""")
  }

  @Test
  fun `only the front-end origin is allowed`() = testApplication {
    application { module() }
    val allowed = client.get("/greet/ada/bye") {
      header(HttpHeaders.Origin, "https://app.example.com")
    }
    assertEquals(HttpStatusCode.OK, allowed.status)
    assertEquals("https://app.example.com", allowed.headers[HttpHeaders.AccessControlAllowOrigin])
    val other = client.get("/greet/ada/bye") {
      header(HttpHeaders.Origin, "https://evil.example.com")
    }
    assertEquals(HttpStatusCode.Forbidden, other.status)
  }
}
