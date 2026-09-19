package routes

import io.ktor.server.html.respondHtml
import io.ktor.server.routing.RoutingContext
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.title

suspend fun RoutingContext.hello(req: Greeting.Hello) {
  val hello = if (req.parent.lang == "nl") "Hallo" else "Hello"
  call.respondHtml {
    head {
      title { +"World Greeting Service" }
    }
    body {
      h1 { +"$hello, ${req.parent.name}, it's ${req.hour} o'clock" }
    }
  }
}
