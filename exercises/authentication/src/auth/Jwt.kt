package auth

import io.ktor.server.application.Application

/**
 * Exercise 4 / 5: issue a token, verify it.
 *
 * `jwtModule` installs `ContentNegotiation` (`json()`), `Resources` and
 * `Authentication` with the `basic("auth")` provider of [basicModule] and a
 * `jwt("auth-jwt")` provider: `realm = "Access to greetings"`,
 * `verifier(JWT.require(Algorithm.HMAC256(secret)).build())`, a
 * `validate { }` that turns a non-empty `username` claim into a
 * `JWTPrincipal(credential.payload)`, and a `challenge { _, _ -> }` that
 * responds `Unauthorized` with the text `Token invalid or expired`.
 *
 * `post("/login")` inside `authenticate("auth") { }` signs a token with
 * `JWT.create()`, the claim `username` (the principal's name), an expiry one
 * minute from now and `Algorithm.HMAC256(secret)`, and responds with
 * `mapOf("token" to token)`. `get<Greeting.Hello>` inside
 * `authenticate("auth-jwt") { }` reads the claim back from
 * `call.principal<JWTPrincipal>()` and responds with the text `Hello, ada`.
 */
fun Application.jwtModule(secret: String): Unit = TODO()

/**
 * Exercise 5 / 5: the secret comes from configuration.
 *
 * `jwtModule()` reads `jwt.secret` from `environment.config` and hands it to
 * [jwtModule]. Nothing in the source knows the secret.
 */
fun Application.jwtModule(): Unit = TODO()
