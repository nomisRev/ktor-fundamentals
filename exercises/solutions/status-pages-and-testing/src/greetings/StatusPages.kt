package greetings

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.p

fun Application.statusPages() {
  install(StatusPages) {
    status(HttpStatusCode.NotFound) { call, status ->
      call.respondHtml(status) {
        body { h1 { +"Keep looking somewhere else" } }
      }
    }
    exception<TooEarly> { call, cause ->
      call.respondHtml(HttpStatusCode.TooEarly) {
        body {
          h1 { +"Come back later" }
          cause.message?.let { p { +it } }
        }
      }
    }
  }
}
