package auth

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

/** The greeting routes of lesson 3. */
@Serializable
@Resource("/greet/{name}")
class Greeting(val name: String, val lang: String? = "en") {
  @Serializable
  @Resource("bye")
  class Bye(val parent: Greeting)

  @Serializable
  @Resource("hello/{hour}")
  class Hello(val parent: Greeting, val hour: Int)
}

/** The session class the login writes and the session provider reads back. */
@Serializable
data class UserInfo(val name: String, val timezone: String)
