package routes

import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.bye(req: Greeting.Bye) {
  call.respondText("Bye, ${req.parent.name}")
}
