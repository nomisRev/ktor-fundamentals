package auth

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.oidc.Oidc
import io.ktor.server.auth.oidc.OpenIdProviderMetadata
import io.ktor.server.auth.oidc.OpenIdTestKeys
import io.ktor.server.auth.principal
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

suspend fun Application.oidcModule(issuerUrl: String, keys: OpenIdTestKeys) {
  install(Resources)
  val oidc = install(Oidc)
  val provider = oidc.identityProvider("test") {
    issuer = issuerUrl
    metadata = OpenIdProviderMetadata(
      issuer = issuerUrl,
      authorizationEndpoint = "$issuerUrl/authorize",
      tokenEndpoint = "$issuerUrl/token",
      jwksUri = "$issuerUrl/jwks",
    )
    jwt(keys)
    bearer { audience = setOf("greetings-api") }
  }
  routing {
    authenticateWith(provider.jwtBearer) {
      get<Greeting.Hello> {
        call.respondText("Hello, ${call.principal.claims.subject}")
      }
    }
  }
}
