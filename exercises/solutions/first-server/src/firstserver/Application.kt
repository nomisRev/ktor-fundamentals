package firstserver

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    get("/hello") {
      call.respondText("Hello, world!")
    }
    post("/hello") {
      call.respondText(
        text = "Created!",
        status = HttpStatusCode.Created,
      )
    }
  }
}

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    module()
  }.start(wait = true)
}
