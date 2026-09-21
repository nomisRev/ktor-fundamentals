package routes

import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.util.getValue

suspend fun RoutingContext.bye() {
  val name: String by call.pathParameters
  call.respondText("Bye, $name")
}
