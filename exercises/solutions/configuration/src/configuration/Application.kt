package configuration

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main(args: Array<String>): Unit =
  io.ktor.server.netty.EngineMain.main(args)

data class Settings(val port: Int, val jwtSecret: String)

fun Application.settings(): Settings = Settings(
  port = environment.config.property("ktor.deployment.port").getString().toInt(),
  jwtSecret = environment.config.property("jwt.secret").getString(),
)

fun Application.module() {
  val settings = settings()
  routing {
    get("/") {
      call.respondText("Greeting service on port ${settings.port}")
    }
  }
}
