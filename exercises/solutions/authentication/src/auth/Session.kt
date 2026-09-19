package auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.auth.principal
import io.ktor.server.auth.session
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

fun Route.login() {
  authenticate("auth") {
    post("/login") {
      val user = call.principal<UserIdPrincipal>()
        ?: return@post call.respond(HttpStatusCode.Unauthorized)
      val timezone = call.request.queryParameters["timezone"] ?: "UTC"
      call.sessions.set(UserInfo(user.name, timezone))
      call.respond(HttpStatusCode.NoContent)
    }
  }
}

fun Application.sessionModule() {
  install(Sessions) {
    cookie<UserInfo>("user", SessionStorageMemory())
  }
  install(Authentication) {
    basic("auth") {
      realm = "Access to greetings"
      validate { checkCredentials(it) }
    }
    session<UserInfo>("auth-session") {
      validate { it }
      challenge { call.respondRedirect("/login") }
    }
  }
  install(Resources)
  routing {
    login()
    authenticate("auth-session") {
      get<Greeting.Hello> {
        val user = call.principal<UserInfo>()
        call.respondText("Hello, ${user?.name} in ${user?.timezone}")
      }
    }
  }
}
