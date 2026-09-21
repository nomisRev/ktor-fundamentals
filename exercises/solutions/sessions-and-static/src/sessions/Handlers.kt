package sessions

import io.ktor.server.html.respondHtml
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.util.getValue
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.img
import kotlinx.html.link
import kotlinx.html.p
import kotlinx.html.title

suspend fun RoutingContext.hello() {
  val name: String by call.pathParameters
  val visits = (call.sessions.get<Visits>()?.count ?: 0) + 1
  call.sessions.set(Visits(visits))
  call.respondHtml {
    head {
      title { +"World Greeting Service" }
      link(rel = "stylesheet", href = "/assets/style.css")
    }
    body {
      img(src = "/assets/logo.svg", alt = "Ktor")
      h1 { +"Hello, $name" }
      p { +"Visit $visits" }
    }
  }
}

suspend fun RoutingContext.bye() {
  val name: String by call.pathParameters
  call.sessions.clear<Visits>()
  call.respondText("Bye, $name")
}
