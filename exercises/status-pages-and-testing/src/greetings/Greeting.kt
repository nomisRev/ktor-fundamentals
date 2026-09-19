package greetings

import io.ktor.resources.Resource
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
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

/** Your own exception: nobody gets greeted before six. */
class TooEarly(val hour: Int) : Exception("It is only $hour o'clock")

/** The routes the status pages, the tests and the metrics are about. */
fun Route.greetings() {
  get<Greeting.Hello> { req ->
    if (req.hour < 6) throw TooEarly(req.hour)
    call.respondText("Hello, ${req.parent.name}, it's ${req.hour} o'clock")
  }
  get<Greeting.Bye> { req ->
    call.respondText("Bye, ${req.parent.name}")
  }
}
