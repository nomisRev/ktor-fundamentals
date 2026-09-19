package greetings

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing

/** The wiring: error pages, metrics, the plug-ins and the greeting routes. */
fun Application.module() {
  statusPages()
  metrics()
  install(Resources)
  install(ContentNegotiation) { json() }
  routing {
    greetings()
  }
}
