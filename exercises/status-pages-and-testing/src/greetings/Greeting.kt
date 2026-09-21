package greetings

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.util.getValue

/** Your own exception: nobody gets greeted before six. */
class TooEarly(val hour: Int) : Exception("It is only $hour o'clock")

/** The greeting routes of lesson 3: the ones the status pages, the tests and the metrics are about. */
fun Route.greetings() {
  route("/greet/{name}") {
    get("hello/{hour}") {
      val name: String by call.pathParameters
      val hour: Int by call.pathParameters
      if (hour < 6) throw TooEarly(hour)
      call.respondText("Hello, $name, it's $hour o'clock")
    }
    get("bye") {
      val name: String by call.pathParameters
      call.respondText("Bye, $name")
    }
  }
}
