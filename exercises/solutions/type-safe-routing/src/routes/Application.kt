package routes

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.routing.routing

fun Application.module() {
  install(Resources)
  routing {
    get<Greeting.Bye> { bye(it) }
    get<Greeting.Hello> { hello(it) }
  }
}
