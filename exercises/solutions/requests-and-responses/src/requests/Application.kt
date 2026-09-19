package requests

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.module() {
  install(ContentNegotiation) { json() }
  routing {
    post("/greet") {
      val greeting = call.receive<Greeting>()
      if (greeting.name.isBlank()) {
        return@post call.respond(HttpStatusCode.BadRequest)
      }
      val hello = when (greeting.type) {
        Type.HELLO -> "Hello, ${greeting.name}"
        Type.BYE -> "Bye, ${greeting.name}"
      }
      val message = greeting.timezone?.let { "$hello in $it" } ?: hello
      call.respond(GreetingResponse(message))
    }
  }
}
