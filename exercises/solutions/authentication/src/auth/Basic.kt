package auth

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.basic
import io.ktor.server.auth.principal
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun checkCredentials(credentials: UserPasswordCredential): UserIdPrincipal? =
  if (credentials.password == "supersecret") UserIdPrincipal(credentials.name)
  else null

val basicAuth = basic<UserIdPrincipal>("auth") {
  realm = "Access to greetings"
  validate { checkCredentials(it) }
}

fun Application.basicModule() {
  install(Resources)
  routing {
    authenticateWith(basicAuth) {
      get<Greeting.Hello> {
        call.respondText("Hello, ${call.principal.name}")
      }
    }
  }
}
