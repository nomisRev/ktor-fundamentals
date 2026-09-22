package configuration

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class Config(val server: Server)

@Serializable
data class Server(val host: String, val port: Int)

fun loadConfig(): Config = ApplicationConfig("application.yaml").getAs<Config>()

fun Application.module(config: Config) {
  routing {
    get("/") {
      call.respondText("Greeting service on ${config.server.host}:${config.server.port}")
    }
  }
}

fun main() {
  val config = loadConfig()
  embeddedServer(
    Netty,
    host = config.server.host,
    port = config.server.port,
  ) { module(config) }.start(wait = true)
}
