package configuration

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/*
 * Exercise 2 / 3: the file as a type.
 *
 * Declare `@Serializable data class Config(val server: Server)` and
 * `@Serializable data class Server(val host: String, val port: Int)`: one
 * property per key of `application.yaml`, the same names. The tests do not
 * compile until both exist.
 */

/**
 * Exercise 3 / 3: load the configuration before the server starts.
 *
 * `ApplicationConfig("application.yaml")` reads the file from the classpath
 * and substitutes the `$VARIABLE` references; `getAs<Config>()` deserialises
 * it with kotlinx.serialization. A missing key is an exception here, not a
 * `null` at the first request.
 */
fun loadConfig(): Config = TODO()

/** The module receives what it needs as an argument; nothing reads the file twice. */
fun Application.module(config: Config) {
  routing {
    get("/") {
      call.respondText("Greeting service on ${config.server.host}:${config.server.port}")
    }
  }
}

/** Load first, fail fast, then start from the loaded values. */
fun main() {
  val config = loadConfig()
  embeddedServer(
    Netty,
    host = config.server.host,
    port = config.server.port,
  ) { module(config) }.start(wait = true)
}
