package routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

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
