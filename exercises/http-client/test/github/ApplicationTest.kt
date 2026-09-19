package github

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.resources.href
import io.ktor.resources.serialization.ResourcesFormat
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

private val ada = User(name = "Ada", bio = "Analytical engines", avatar_url = null)
private val ktor = Repo(name = "ktor", description = "Kotlin HTTP", fork = false, stargazers_count = 13)

/** A fake service: `getUserInfo` only returns once `getUserRepos` has started, and vice versa. */
private class FakeGitHub : GitHubService {
  private val infoStarted = CompletableDeferred<Unit>()
  private val reposStarted = CompletableDeferred<Unit>()

  override suspend fun getUserInfo(user: String): User? {
    infoStarted.complete(Unit)
    reposStarted.await()
    return if (user == "ada") ada else null
  }

  override suspend fun getUserRepos(user: String): List<Repo>? {
    reposStarted.complete(Unit)
    infoStarted.await()
    return if (user == "ada") listOf(ktor) else null
  }
}

/** GitHub, mocked: `/users/ada` and `/users/ada/repos` answer, everything else is a 404. */
private fun ApplicationTestBuilder.mockGitHub(): HttpClient {
  externalServices {
    hosts("https://api.github.com") {
      install(ServerContentNegotiation) { json() }
      routing {
        get("/users/ada") { call.respond(ada) }
        get("/users/ada/repos") { call.respond(listOf(ktor)) }
        get("/users/{username}") { call.respond(HttpStatusCode.NotFound) }
        get("/users/{username}/repos") { call.respond(HttpStatusCode.NotFound) }
      }
    }
  }
  return createClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Resources)
    defaultRequest { url("https://api.github.com") }
  }
}

class ApplicationTest {
  @Test
  fun `the resources describe GitHub's paths`() {
    val format = ResourcesFormat()
    assertEquals("/users/ada", href(format, GitHub.User(username = "ada")))
    assertEquals("/users/ada/repos", href(format, GitHub.User.Repos(user = GitHub.User(username = "ada"))))
  }

  @Test
  fun `the client has its plug-ins`() {
    client().use { client ->
      assertNotNull(client.pluginOrNull(ContentNegotiation), "ContentNegotiation is not installed")
      assertNotNull(client.pluginOrNull(Resources), "Resources is not installed")
      assertNotNull(client.pluginOrNull(DefaultRequest), "defaultRequest is not configured")
      assertNotNull(client.pluginOrNull(HttpRequestRetry), "HttpRequestRetry is not installed")
    }
  }

  @Test
  fun `profile fetches info and repos concurrently`() = testApplication {
    application {
      install(ServerContentNegotiation) { json() }
      routing {
        get("/profile/{name}") { profile(FakeGitHub(), call.parameters["name"]!!) }
      }
    }
    val response = withTimeoutOrNull(5.seconds) { client.get("/profile/ada") }
    assertNotNull(response, "getUserInfo and getUserRepos did not run concurrently")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(Profile(ada, listOf(ktor)), Json.decodeFromString<Profile>(response.bodyAsText()))
  }

  @Test
  fun `an unknown user is a 404`() = testApplication {
    application {
      install(ServerContentNegotiation) { json() }
      routing {
        get("/profile/{name}") { profile(FakeGitHub(), call.parameters["name"]!!) }
      }
    }
    assertEquals(HttpStatusCode.NotFound, client.get("/profile/nobody").status)
  }

  @Test
  fun `GitHubHttp reads the JSON`() = testApplication {
    val github = GitHubHttp(mockGitHub())
    assertEquals(ada, github.getUserInfo("ada"))
    assertEquals(listOf(ktor), github.getUserRepos("ada"))
    assertNull(github.getUserInfo("nobody"))
    assertNull(github.getUserRepos("nobody"))
  }

  @Test
  fun `GitHubHttp closes its client`() = testApplication {
    val client = mockGitHub()
    GitHubHttp(client).close()
    assertFalse(client.isActive, "the client is still active after close()")
  }

  @Test
  fun `the module provides the service through DI`() = testApplication {
    val github = mockGitHub()
    application { module(client = github) }
    val response = client.get("/profile/ada")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(Profile(ada, listOf(ktor)), Json.decodeFromString<Profile>(response.bodyAsText()))
    assertEquals(HttpStatusCode.NotFound, client.get("/profile/nobody").status)
  }

  @Test
  fun `a fake service answers the same route`() = testApplication {
    application {
      dependencies { provide<GitHubService> { FakeGitHub() } }
      routes()
    }
    val response = client.get("/profile/ada")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(Profile(ada, listOf(ktor)), Json.decodeFromString<Profile>(response.bodyAsText()))
  }
}
