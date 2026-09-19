package sessions

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie

val signKey: ByteArray = "6819b57a326945c1968f45236589".hexToByteArray()

fun Application.sessions() {
  install(Sessions) {
    cookie<Visits>("visits") {
      cookie.path = "/"
      transform(SessionTransportTransformerMessageAuthentication(signKey))
    }
  }
}
