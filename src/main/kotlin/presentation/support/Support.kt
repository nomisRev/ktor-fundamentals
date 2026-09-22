// Hand-written context for the generated snippets in ../snippets/.
//
// The slides use these DTOs, services, and values without defining
// them on every slide; each generated file star-imports this package. A snippet
// that defines its own `Greeting` or `routes` shadows the one here, because a
// declaration in the file's own package wins over a star import.
package presentation.support

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.AuthenticationRole
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.basic
import io.ktor.server.auth.form
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.oidc.OidcProvider
import io.ktor.server.auth.session
import io.ktor.server.auth.withRoles
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue
import io.ktor.server.testing.testApplication
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// Requests and responses (lesson 2)

/** The body of `POST /greet`; the lesson 2 slides that define it shadow this copy. */
@Serializable
data class Greeting(val name: String)

// ---------------------------------------------------------------------------
// GitHub DTOs and service (lesson 5)

@Serializable
data class Repo(
  val name: String,
  val description: String?,
  val fork: Boolean,
  val stargazers_count: Int,
)

@Serializable
data class User(
  val name: String?,
  val bio: String?,
  val avatar_url: String?,
)

interface GitHubService {
  suspend fun getUserInfo(user: String): User?
  suspend fun getUserRepos(user: String): List<Repo>?
}

interface CacheService {
  suspend fun get(key: String): String?
  suspend fun update(key: String, value: String)
}

/** The page a handler in lesson 5 answers with: user info plus repositories. */
@Serializable
data class Profile(val user: User, val repos: List<Repo>)

/** The client factory the lesson 5 slides build step by step. */
fun client(): HttpClient = HttpClient(CIO) {
  install(ContentNegotiation) {
    json(Json { ignoreUnknownKeys = true })
  }
  defaultRequest { url("https://api.github.com") }
}

/** The HTTP implementation of [GitHubService] the DI slides provide. */
class GitHubHttp(
  private val client: HttpClient,
) : GitHubService, AutoCloseable {
  override suspend fun getUserInfo(user: String): User? =
    client.get("/users/$user")
      .takeIf { it.status == HttpStatusCode.OK }
      ?.body<User>()

  override suspend fun getUserRepos(user: String): List<Repo>? =
    client.get("/users/$user/repos")
      .takeIf { it.status == HttpStatusCode.OK }
      ?.body<List<Repo>>()

  override fun close() = client.close()
}

/** The handler the DI slides wire up: the one from "Handlers depend on the service". */
suspend fun RoutingContext.github(github: GitHubService, user: String) {
  val info = github.getUserInfo(user)
  if (info == null) call.respond(HttpStatusCode.NotFound)
  else call.respond(Profile(info, github.getUserRepos(user).orEmpty()))
}

/** The route the DI slides register: `Routing.profile` from "Handlers depend on the service". */
fun Routing.profile(github: GitHubService) {
  get("/profile") {
    val user: String by call.queryParameters
    github(github, user)
  }
}

// ---------------------------------------------------------------------------
// Small helpers the slides refer to without defining

interface Logger {
  suspend fun log(message: String)
}

class DatabaseException(override val message: String) : Exception(message)

/** Placeholder route registration used by `module()`-style snippets. */
fun Application.routes() {
  routing { }
}

// Values the login examples read without introducing them on the slide.
val loggedIn: Boolean = true
val name: String = "Alex"
val tz: String = "CET"
val secret: String = "supersecret"

// Names the response examples in lesson 2 look up before answering `404`.
val users: Set<String> = setOf("Alex")

/** The login route of lesson 8 as one call, for the slides that only wrap it. */
suspend fun RoutingContext.issueToken() {
  val token = JWT.create().withClaim("username", name).sign(Algorithm.HMAC256(secret))
  call.respond(mapOf("token" to token))
}

// ---------------------------------------------------------------------------
// Authentication (lesson 8)

/** The session class the login writes and the session provider reads back. */
@Serializable
data class UserInfo(val name: String, val timezone: String)

/** The validation function the schemes share: a principal, or `null`. */
fun checkCredentials(credentials: UserPasswordCredential): UserIdPrincipal? =
  if (credentials.password == "supersecret") UserIdPrincipal(credentials.name)
  else null

// The schemes the route slides pass to `authenticateWith` without repeating
// their definition. The slide that defines one shadows this copy.
val basicAuth = basic<UserIdPrincipal>("auth") {
  realm = "Access to secrets"
  validate { checkCredentials(it) }
}

val formAuth = form<UserIdPrincipal>("auth-form") {
  usernameField = "username"
  passwordField = "password"
  validate { checkCredentials(it) }
}

val sessionAuth = session<UserInfo, UserIdPrincipal>("auth-session") {
  validate { info -> if (info.name in users) UserIdPrincipal(info.name) else null }
}

val jwtAuth = jwt<UserIdPrincipal>("auth-jwt") {
  realm = "Access to secrets"
  verifier(JWT.require(Algorithm.HMAC256(secret)).build())
  validate { credential ->
    val user = credential.payload.getClaim("username").asString()
    if (user.isNullOrEmpty()) null else UserIdPrincipal(user)
  }
}

/** The roles the authorization slides check; an enum's `name` satisfies `AuthenticationRole`. */
enum class Role : AuthenticationRole { User, Admin }

val roleAuth = jwtAuth.withRoles { user ->
  if (user.name == "Alex") setOf(Role.User, Role.Admin) else setOf(Role.User)
}

/** The OpenID Connect provider the route slides protect with; the module slides register it. */
lateinit var google: OidcProvider

// ---------------------------------------------------------------------------
// WebSockets and OpenAPI (lesson 6)

/** One frame in, one frame out: the JSON messages on the `/greet` socket. */
@Serializable
data class Request(val name: String)

@Serializable
data class Response(val greeting: String)

/** The body the documented greeting route answers with. */
@Serializable
data class GreetingResponse(val message: String)

/** The greeting handler the OpenAPI slides document without repeating its body. */
suspend fun RoutingContext.greet() {
  val name = call.parameters["name"]
    ?: return call.respond(HttpStatusCode.BadRequest)
  val hello = if (call.request.queryParameters["lang"] == "nl") "Hallo" else "Hello"
  call.respond(GreetingResponse("$hello, $name"))
}

// ---------------------------------------------------------------------------
// Status pages, testing and metrics (lesson 7)

/** The application the lesson 7 tests start: the greeting routes of lesson 3. */
fun Application.module() {
  routing {
    route("/greet/{name}") {
      get("bye") {
        val name: String by call.pathParameters
        call.respondText("Bye, $name")
      }
      get("hello/{hour}") {
        val name: String by call.pathParameters
        val hour: Int by call.pathParameters
        call.respondText("Hello, $name, it's $hour o'clock")
      }
    }
  }
}

/** The same application plus the GitHub profile route of lesson 5, behind [github]. */
fun Application.module(github: GitHubService) {
  module()
  install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { json() }
  routing {
    get("/github/{username}") {
      val username: String by call.pathParameters
      github(github, username)
    }
  }
}

/** The marker a handler responds with when it wants the "say please" page. */
@Serializable
object SayPlease

/** The plug-in of the custom status page slides, so that `install(FourOhFour)` compiles. */
val FourOhFour = createApplicationPlugin("FourOhFour") {
  onCallRespond { call ->
    transformBody { body ->
      when (body) {
        is SayPlease -> TextContent(
          text = createHTML().html { body { h1 { +"Say please" } } },
          contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8),
          status = HttpStatusCode.Forbidden,
        )
        else -> body
      }
    }
  }
}

/** The test helper the testing slides build: application plus a configured client. */
fun appTest(test: suspend (HttpClient) -> Unit) = testApplication {
  application { module() }
  val client = createClient {
    install(ContentNegotiation) { json() }
    expectSuccess = true
  }
  test(client)
}
