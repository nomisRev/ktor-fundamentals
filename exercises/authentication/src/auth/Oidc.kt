package auth

import io.ktor.server.application.Application
import io.ktor.server.auth.oidc.OpenIdTestKeys

/**
 * Exercise 7 / 7: an API validates the tokens it is handed.
 *
 * `oidcModule` installs `Resources` and `Oidc`, and registers an identity
 * provider named `test` whose `issuer` is [issuerUrl]. There is no provider
 * to discover in a test, so it sets static `metadata`, an
 * `OpenIdProviderMetadata` with the issuer and `$issuerUrl/authorize`,
 * `$issuerUrl/token` and `$issuerUrl/jwks` as endpoints, and `jwt(keys)`, which
 * verifies signatures against the in-memory key pair instead of fetching JWKS.
 * `bearer { audience = setOf("greetings-api") }` names the API.
 *
 * `get<Greeting.Hello>` inside `authenticateWith(provider.jwtBearer) { }`
 * responds with `Hello, ada`, the token's `call.principal.claims.subject`.
 * A token for another audience, and no token at all, answer `401`.
 *
 * `identityProvider` suspends, so the module is a `suspend fun`.
 */
suspend fun Application.oidcModule(issuerUrl: String, keys: OpenIdTestKeys): Unit = TODO()
