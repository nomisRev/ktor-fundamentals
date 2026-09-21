package auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.auth.withRoles
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.Date

fun jwtAuth(secret: String) = jwt<UserIdPrincipal>("auth-jwt") {
  realm = "Access to greetings"
  verifier(JWT.require(Algorithm.HMAC256(secret)).build())
  validate { credential ->
    val user = credential.payload.getClaim("username").asString()
    if (user.isNullOrEmpty()) null else UserIdPrincipal(user)
  }
  onUnauthorized = {
    call.respond(HttpStatusCode.Unauthorized, "Token invalid or expired")
  }
}

private fun Route.issueToken(secret: String) {
  authenticateWith(basicAuth) {
    post("/login") {
      val token = JWT.create()
        .withClaim("username", call.principal.name)
        .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
        .sign(Algorithm.HMAC256(secret))
      call.respond(mapOf("token" to token))
    }
  }
}

fun Application.jwtModule(secret: String) {
  install(ContentNegotiation) { json() }
  routing {
    issueToken(secret)
    authenticateWith(jwtAuth(secret)) {
      get("/greet/{name}/hello/{hour}") {
        call.respondText("Hello, ${call.principal.name}")
      }
    }
  }
}

fun Application.adminModule(secret: String) {
  install(ContentNegotiation) { json() }
  val adminAuth = jwtAuth(secret).withRoles { user ->
    if (user.name == "ada") setOf(Role.Admin, Role.User) else setOf(Role.User)
  }
  routing {
    issueToken(secret)
    authenticateWith(adminAuth, roles = setOf(Role.Admin)) {
      get("/greet/{name}/hello/{hour}") {
        call.respondText("Hello, ${call.principal.name}")
      }
    }
  }
}

fun Application.jwtModule() {
  val secret = environment.config.property("jwt.secret").getString()
  jwtModule(secret)
}
