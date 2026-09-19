package requests

import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class ApplicationTest {
  @Test
  fun `a greeting serializes with short names`() {
    val greeting = Greeting(type = Type.BYE, name = "Ada", timezone = "CET")
    assertEquals("""{"type":"bye","name":"Ada","tz":"CET"}""", Json.encodeToString(greeting))
    assertEquals("""{"message":"Hello, Ada"}""", Json.encodeToString(GreetingResponse("Hello, Ada")))
  }

  @Test
  fun `type and timezone have defaults`() {
    assertEquals(Greeting(Type.HELLO, "Ada", null), Json.decodeFromString<Greeting>("""{"name":"Ada"}"""))
  }

  @Test
  fun `post greet answers with JSON`() = testApplication {
    application { module() }
    val response = client.post("/greet") {
      contentType(ContentType.Application.Json)
      setBody("""{"name":"Ada"}""")
    }
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
    assertEquals("""{"message":"Hello, Ada"}""", response.bodyAsText())
  }

  @Test
  fun `bye says bye`() = testApplication {
    application { module() }
    val response = client.post("/greet") {
      contentType(ContentType.Application.Json)
      setBody("""{"type":"bye","name":"Linus"}""")
    }
    assertEquals("""{"message":"Bye, Linus"}""", response.bodyAsText())
  }

  @Test
  fun `a time zone ends up in the message`() = testApplication {
    application { module() }
    val response = client.post("/greet") {
      contentType(ContentType.Application.Json)
      setBody("""{"name":"Grace","tz":"CET"}""")
    }
    assertEquals("""{"message":"Hello, Grace in CET"}""", response.bodyAsText())
  }

  @Test
  fun `a blank name is a 400`() = testApplication {
    application { module() }
    val response = client.post("/greet") {
      contentType(ContentType.Application.Json)
      setBody("""{"name":"   "}""")
    }
    assertEquals(HttpStatusCode.BadRequest, response.status)
    assertEquals("", response.bodyAsText())
  }

  @Test
  fun `xml is not acceptable without xml()`() = testApplication {
    application { module() }
    val response = client.post("/greet") {
      contentType(ContentType.Application.Json)
      accept(ContentType.Application.Xml)
      setBody("""{"name":"Olivia"}""")
    }
    assertEquals(HttpStatusCode.NotAcceptable, response.status)
  }
}
