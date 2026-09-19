package sessions

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

/** The greeting routes of lesson 3, without the hour this time. */
@Serializable
@Resource("/greet/{name}")
class Greeting(val name: String) {
  @Serializable
  @Resource("hello")
  class Hello(val parent: Greeting)

  @Serializable
  @Resource("bye")
  class Bye(val parent: Greeting)
}

/** The session: how often this visitor said hello. */
@Serializable
data class Visits(val count: Int)
