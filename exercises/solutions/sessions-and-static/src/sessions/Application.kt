package sessions

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Route.assets() {
  staticResources("/assets", "static")
}

fun Application.cors() {
  install(CORS) {
    allowHost("app.example.com", schemes = listOf("https"))
  }
}

fun Application.module() {
  sessions()
  cors()
  routing {
    assets()
    route("/greet/{name}") {
      get("hello") { hello() }
      get("bye") { bye() }
    }
  }
}
