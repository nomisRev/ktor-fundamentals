package auth

import io.ktor.server.application.Application
import io.ktor.server.auth.SimpleAuthenticationScheme
import io.ktor.server.auth.UserIdPrincipal

/**
 * Exercise 4 / 7: verify the signature, then the claims.
 *
 * `jwtAuth` is a `jwt<UserIdPrincipal>("auth-jwt")` scheme:
 * `realm = "Access to greetings"`,
 * `verifier(JWT.require(Algorithm.HMAC256(secret)).build())`, a `validate { }`
 * that turns a non-empty `username` claim into a `UserIdPrincipal`, and an
 * `onUnauthorized` handler that responds `Unauthorized` with the text
 * `Token invalid or expired`. A function rather than a `val`, because the
 * secret is a parameter.
 */
fun jwtAuth(secret: String): SimpleAuthenticationScheme<UserIdPrincipal> = TODO()

/**
 * Exercise 4 / 7: issue a token, verify it.
 *
 * `jwtModule` installs `ContentNegotiation` (`json()`) and `Resources`.
 * `post("/login")` inside `authenticateWith(basicAuth) { }` signs a token
 * with `JWT.create()`, the claim `username` (`call.principal.name`), an
 * expiry one minute from now and `Algorithm.HMAC256(secret)`, and responds
 * with `mapOf("token" to token)`. `get<Greeting.Hello>` inside
 * `authenticateWith(jwtAuth(secret)) { }` responds with the text `Hello, ada`,
 * the principal's name.
 */
fun Application.jwtModule(secret: String): Unit = TODO()

/**
 * Exercise 5 / 7: a missing role is `403`, not `401`.
 *
 * `adminModule` is [jwtModule] with a role check on the greeting: the scheme
 * is `jwtAuth(secret).withRoles { user -> … }`, where `ada` has
 * `Role.Admin` and `Role.User` and everyone else only `Role.User`, and the
 * greeting sits inside `authenticateWith(adminAuth, roles = setOf(Role.Admin)) { }`.
 * The handler responds with `Hello, ada`; a valid token for anyone else
 * gets `403 Forbidden`.
 */
fun Application.adminModule(secret: String): Unit = TODO()

/**
 * Exercise 6 / 7: the secret comes from configuration.
 *
 * `jwtModule()` reads `jwt.secret` from `environment.config` and hands it to
 * [jwtModule]. Nothing in the source knows the secret.
 */
fun Application.jwtModule(): Unit = TODO()
