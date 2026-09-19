package auth

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.auth.principal
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun checkCredentials(credentials: UserPasswordCredential): UserIdPrincipal? =
  if (credentials.password == "supersecret") UserIdPrincipal(credentials.name)
  else null

fun Application.basicModule() {
  install(Authentication) {
    basic("auth") {
      realm = "Access to greetings"
      validate { checkCredentials(it) }
    }
  }
  install(Resources)
  routing {
    authenticate("auth") {
      get<Greeting.Hello> {
        val user = call.principal<UserIdPrincipal>()
        call.respondText("Hello, ${user?.name}")
      }
    }
  }
}
