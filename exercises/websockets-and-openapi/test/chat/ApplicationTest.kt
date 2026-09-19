package chat

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApplicationTest {
  @Test
  fun `chat echoes the message with a timestamp`() = testApplication {
    application {
      webSockets()
      routing { chat() }
    }
    val client = createClient {
      install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
      }
    }
    client.webSocket("/chat") {
      val before = System.currentTimeMillis()
      sendSerialized(ChatMessage("Hello, Ada"))
      val echo = receiveDeserialized<ChatMessage>()
      assertEquals("Hello, Ada", echo.text)
      val timestamp = assertNotNull(echo.timestamp, "the echo carries no timestamp")
      assertTrue(timestamp in before..System.currentTimeMillis(), "timestamp $timestamp is not now")

      sendSerialized(ChatMessage("Hello, Linus"))
      assertEquals("Hello, Linus", receiveDeserialized<ChatMessage>().text)
    }
  }

  @Test
  fun `greeting answers with JSON`() = testApplication {
    application {
      install(ContentNegotiation) { json() }
      routing { greeting() }
    }
    assertEquals("""{"message":"Hello, ada"}""", client.get("/greet/ada").bodyAsText())
    assertEquals("""{"message":"Hallo, grace"}""", client.get("/greet/grace?lang=nl").bodyAsText())
  }

  @Test
  fun `the document describes the greeting route`() = testApplication {
    application {
      install(ContentNegotiation) { json() }
      routing {
        greeting()
        docs()
      }
    }
    val response = client.get("/swagger/openapi.json")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
    val document = Json.parseToJsonElement(response.bodyAsText()).jsonObject
    assertEquals("Greeting API", document["info"]?.jsonObject?.get("title")?.jsonPrimitive?.content)
    val paths = assertNotNull(document["paths"]?.jsonObject)
    val get = assertNotNull(paths["/greet/{name}"]?.jsonObject?.get("get")?.jsonObject, "no GET /greet/{name}")
    assertEquals("Greet a person by name", get["summary"]?.jsonPrimitive?.content)
    val parameters = get["parameters"].toString()
    assertContains(parameters, "\"lang\"")
    assertContains(parameters, "Language of the greeting")
    assertContains(get["responses"].toString(), "\"200\"")
    assertContains(get["responses"].toString(), "#/components/schemas/GreetingResponse")
    val schema = document["components"]?.jsonObject?.get("schemas")?.jsonObject?.get("GreetingResponse")
    assertContains(assertNotNull(schema, "no GreetingResponse schema").toString(), "\"message\"")
  }

  @Test
  fun `the health check is hidden`() = testApplication {
    application {
      install(ContentNegotiation) { json() }
      routing {
        greeting()
        docs()
      }
    }
    assertEquals("ok", client.get("/health").bodyAsText())
    val paths = Json.parseToJsonElement(client.get("/swagger/openapi.json").bodyAsText())
      .jsonObject["paths"]?.jsonObject.orEmpty()
    assertFalse("/health" in paths, "/health is in the document: ${paths.keys}")
  }

  @Test
  fun `swagger UI is served`() = testApplication {
    application { module() }
    val response = client.get("/swagger")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
    assertContains(response.bodyAsText(), "swagger-ui")
  }
}
