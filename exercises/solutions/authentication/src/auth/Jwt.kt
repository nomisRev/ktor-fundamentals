package auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.Date

fun Application.jwtModule(secret: String) {
  install(ContentNegotiation) { json() }
  install(Resources)
  install(Authentication) {
    basic("auth") {
      realm = "Access to greetings"
      validate { checkCredentials(it) }
    }
    jwt("auth-jwt") {
      realm = "Access to greetings"
      verifier(JWT.require(Algorithm.HMAC256(secret)).build())
      validate { credential ->
        val user = credential.payload.getClaim("username").asString()
        if (!user.isNullOrEmpty()) JWTPrincipal(credential.payload) else null
      }
      challenge { _, _ ->
        call.respond(HttpStatusCode.Unauthorized, "Token invalid or expired")
      }
    }
  }
  routing {
    authenticate("auth") {
      post("/login") {
        val user = call.principal<UserIdPrincipal>()
          ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val token = JWT.create()
          .withClaim("username", user.name)
          .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
          .sign(Algorithm.HMAC256(secret))
        call.respond(mapOf("token" to token))
      }
    }
    authenticate("auth-jwt") {
      get<Greeting.Hello> {
        val principal = call.principal<JWTPrincipal>()
        val user = principal?.payload?.getClaim("username")?.asString()
        call.respondText("Hello, $user")
      }
    }
  }
}

fun Application.jwtModule() {
  val secret = environment.config.property("jwt.secret").getString()
  jwtModule(secret)
}
