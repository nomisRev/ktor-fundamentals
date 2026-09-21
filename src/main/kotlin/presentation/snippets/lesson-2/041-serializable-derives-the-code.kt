import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class GreetingResponse(val message: String)

fun interface UserService {
  operator fun contains(name: String?): Boolean
}

// Example
fun Application.module(users: UserService) {
  routing {
    post("/greet/{name}") {
      val name = call.parameters["name"]
      if (name !in users) call.respond(HttpStatusCode.NotFound)
      else call.respond(HttpStatusCode.Created, GreetingResponse("Hello, $name"))
    }
  }
}
