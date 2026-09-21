package auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.install
import io.ktor.server.auth.principal
import io.ktor.server.auth.session
import io.ktor.server.auth.setSession
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Route.login() {
  authenticateWith(basicAuth) {
    post("/login") {
      val timezone = call.request.queryParameters["timezone"] ?: "UTC"
      sessionAuth.setSession(UserInfo(call.principal.name, timezone))
      call.respond(HttpStatusCode.NoContent)
    }
  }
}

val sessionAuth = session<UserInfo, UserIdPrincipal>("auth-session") {
  validate { UserIdPrincipal(it.name) }
  onUnauthorized = { call.respondRedirect("/login") }
}

fun Application.sessionModule() {
  install(sessionAuth)
  routing {
    login()
    authenticateWith(sessionAuth) {
      get("/greet/{name}/hello/{hour}") {
        call.respondText("Hello, ${call.principal.name} in ${call.session.timezone}")
      }
    }
  }
}
