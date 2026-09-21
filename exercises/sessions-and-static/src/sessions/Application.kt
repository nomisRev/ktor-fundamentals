package sessions

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing

/**
 * Exercise 3 / 4: a folder is a route.
 *
 * `assets` serves the `static` folder of the class path (this module's
 * `resources/static`) at `/assets` with `staticResources`, so that
 * `/assets/style.css` and `/assets/logo.svg` answer.
 */
fun Route.assets(): Unit = TODO()

/**
 * Exercise 4 / 4: one origin is allowed.
 *
 * `cors` installs `CORS` and allows only the front-end at
 * `https://app.example.com` (`allowHost` with `schemes = listOf("https")`).
 * A request from another origin is refused by the browser, because the
 * server answers it with `403 Forbidden` and no `Access-Control-Allow-Origin`.
 */
fun Application.cors(): Unit = TODO()

/** The wiring: the plug-ins above, the assets and the two greeting routes. */
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
