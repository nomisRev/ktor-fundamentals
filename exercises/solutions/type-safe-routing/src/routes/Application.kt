package routes

import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    route("/greet/{name}") {
      get("bye") { bye() }
      get("hello/{hour}") { hello() }
    }
  }
}
