package auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.setCookie
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private suspend fun HttpResponse.token(): String =
  assertNotNull(Json.parseToJsonElement(bodyAsText()).jsonObject["token"]?.jsonPrimitive?.content, "no token in $this")

class AuthenticationTest {
  @Test
  fun `checkCredentials knows the secret`() {
    assertEquals("ada", checkCredentials(UserPasswordCredential("ada", "supersecret"))?.name)
    assertNull(checkCredentials(UserPasswordCredential("ada", "password")))
  }

  @Test
  fun `basic challenges without credentials`() = testApplication {
    application { basicModule() }
    val response = client.get("/greet/ada/hello/9")
    assertEquals(HttpStatusCode.Unauthorized, response.status)
    assertContains(assertNotNull(response.headers[HttpHeaders.WWWAuthenticate]), "Basic realm=\"Access to greetings\"")
  }

  @Test
  fun `basic greets the principal`() = testApplication {
    application { basicModule() }
    val response = client.get("/greet/ada/hello/9") { basicAuth("linus", "supersecret") }
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello, linus", response.bodyAsText())
    val wrong = client.get("/greet/ada/hello/9") { basicAuth("linus", "password") }
    assertEquals(HttpStatusCode.Unauthorized, wrong.status)
  }

  @Test
  fun `login sets the session cookie`() = testApplication {
    application { sessionModule() }
    val response = client.post("/login?timezone=CET") { basicAuth("ada", "supersecret") }
    assertEquals(HttpStatusCode.NoContent, response.status)
    assertNotNull(response.setCookie().singleOrNull { it.name == "user" }, "no `user` cookie was set")
    assertEquals(HttpStatusCode.Unauthorized, client.post("/login").status)
  }

  @Test
  fun `the session guards hello and redirects to login`() = testApplication {
    application { sessionModule() }
    val client = createClient {
      install(HttpCookies)
      followRedirects = false
    }
    val anonymous = client.get("/greet/ada/hello/9")
    assertEquals(HttpStatusCode.Found, anonymous.status)
    assertEquals("/login", anonymous.headers[HttpHeaders.Location])

    client.post("/login?timezone=CET") { basicAuth("ada", "supersecret") }
    val response = client.get("/greet/ada/hello/9")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello, ada in CET", response.bodyAsText())
  }

  @Test
  fun `login issues a signed token`() = testApplication {
    application { jwtModule("test-secret") }
    val response = client.post("/login") { basicAuth("grace", "supersecret") }
    assertEquals(HttpStatusCode.OK, response.status)
    val token = response.token()
    val decoded = JWT.require(Algorithm.HMAC256("test-secret")).build().verify(token)
    assertEquals("grace", decoded.getClaim("username").asString())
    assertNotNull(decoded.expiresAt, "the token never expires")
    assertEquals(HttpStatusCode.Unauthorized, client.post("/login").status)
  }

  @Test
  fun `jwt guards hello`() = testApplication {
    application { jwtModule("test-secret") }
    val token = client.post("/login") { basicAuth("olivia", "supersecret") }.token()
    val response = client.get("/greet/ada/hello/9") { bearerAuth(token) }
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello, olivia", response.bodyAsText())

    val forged = JWT.create().withClaim("username", "olivia").sign(Algorithm.HMAC256("other-secret"))
    val refused = client.get("/greet/ada/hello/9") { bearerAuth(forged) }
    assertEquals(HttpStatusCode.Unauthorized, refused.status)
    assertEquals("Token invalid or expired", refused.bodyAsText())
    assertEquals(HttpStatusCode.Unauthorized, client.get("/greet/ada/hello/9").status)
  }

  @Test
  fun `the secret comes from configuration`() = testApplication {
    environment {
      config = MapApplicationConfig("jwt.secret" to "from-config")
    }
    application { jwtModule() }
    val token = client.post("/login") { basicAuth("ada", "supersecret") }.token()
    JWT.require(Algorithm.HMAC256("from-config")).build().verify(token)
    assertFailsWith<JWTVerificationException> {
      JWT.require(Algorithm.HMAC256("supersecret")).build().verify(token)
    }
  }
}
