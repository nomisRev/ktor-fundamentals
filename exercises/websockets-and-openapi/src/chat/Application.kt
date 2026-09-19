package chat

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

/** The wiring: the socket, the documented route and the document. */
fun Application.module() {
  webSockets()
  install(ContentNegotiation) { json() }
  routing {
    chat()
    greeting()
    docs()
  }
}
