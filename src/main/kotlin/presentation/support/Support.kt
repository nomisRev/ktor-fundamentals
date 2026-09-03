// Hand-written context for the generated snippets in ../snippets/.
//
// The slides use these resources, DTOs, services, and values without defining
// them on every slide; each generated file star-imports this package. A snippet
// that defines its own `Greeting` or `routes` shadows the one here, because a
// declaration in the file's own package wins over a star import.
package presentation.support

import io.ktor.resources.Resource
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Type-safe routes shared across lessons

@Serializable
@Resource("/greet/{name}")
class Greeting(val name: String, val lang: String? = "en") {
  @Serializable
  @Resource("bye")
  class Bye(val parent: Greeting) {
    companion object {
      operator fun invoke(name: String): Bye = Bye(Greeting(name))
    }
  }

  @Serializable
  @Resource("hello/{hour}")
  class Hello(val parent: Greeting, val hour: Int)
}

@Serializable
@Resource("/spell/{name}")
class Spell(val name: String)

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
