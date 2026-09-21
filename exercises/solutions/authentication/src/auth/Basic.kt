package auth

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.basic
import io.ktor.server.auth.principal
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun checkCredentials(credentials: UserPasswordCredential): UserIdPrincipal? =
  if (credentials.password == "supersecret") UserIdPrincipal(credentials.name)
  else null

val basicAuth = basic<UserIdPrincipal>("auth") {
  realm = "Access to greetings"
  validate { checkCredentials(it) }
}

fun Application.basicModule() {
  routing {
    authenticateWith(basicAuth) {
      get("/greet/{name}/hello/{hour}") {
        call.respondText("Hello, ${call.principal.name}")
      }
    }
  }
}
