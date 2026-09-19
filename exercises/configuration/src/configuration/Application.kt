package configuration

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/** `EngineMain` reads `application.yaml` and loads the modules it lists. */
fun main(args: Array<String>): Unit =
  io.ktor.server.netty.EngineMain.main(args)

/** What the module needs from the file: the port it listens on and the JWT secret. */
data class Settings(val port: Int, val jwtSecret: String)

/**
 * Exercise 2 / 2: the module reads its own keys.
 *
 * `settings` builds a [Settings] from `environment.config`:
 * `property("ktor.deployment.port").getString().toInt()` and
 * `property("jwt.secret").getString()`. A missing key is an error at start-up,
 * not a `null` at the first request.
 *
 * The remaining steps of the exercise slide are not code: build the image
 * with `./gradlew buildImage`, run it with `./gradlew runDocker`, then start
 * it again with a different `PORT`. See README.md.
 */
fun Application.settings(): Settings = TODO()

/** The module `application.yaml` lists: one route that shows what was read. */
fun Application.module() {
  val settings = settings()
  routing {
    get("/") {
      call.respondText("Greeting service on port ${settings.port}")
    }
  }
}
