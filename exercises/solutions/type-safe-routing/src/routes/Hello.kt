package routes

import io.ktor.server.html.respondHtml
import io.ktor.server.routing.RoutingContext
import io.ktor.server.util.getValue
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.title

suspend fun RoutingContext.hello() {
  val name: String by call.pathParameters
  val hour by call.hour()
  val lang: String? by call.queryParameters
  val hello = if (lang == "nl") "Hallo" else "Hello"
  call.respondHtml {
    head {
      title { +"World Greeting Service" }
    }
    body {
      h1 { +"$hello, $name, it's ${hour.value} o'clock" }
    }
  }
}
