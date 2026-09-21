package auth

import io.ktor.server.application.Application
import io.ktor.server.auth.SimpleAuthenticationScheme
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential

/**
 * Exercise 1 / 7: validation returns a principal or `null`.
 *
 * `checkCredentials` accepts any name whose password is `supersecret` and
 * returns a `UserIdPrincipal` with that name, `null` otherwise. Every
 * scheme in this module validates with it.
 */
fun checkCredentials(credentials: UserPasswordCredential): UserIdPrincipal? = TODO()

/**
 * Exercise 1 / 7: a scheme is a value.
 *
 * `basicAuth` is a `basic<UserIdPrincipal>("auth")` scheme with
 * `realm = "Access to greetings"` and `validate { checkCredentials(it) }`.
 * Write it as a plain `val`: the other modules pass the same value to
 * `authenticateWith`.
 */
val basicAuth: SimpleAuthenticationScheme<UserIdPrincipal>
  get() = TODO()

/**
 * Exercise 1 / 7: guard with `basic`.
 *
 * `basicModule` installs `Resources` and registers `get<Greeting.Hello>` inside
 * `authenticateWith(basicAuth) { }`. The handler reads `call.principal`, a
 * non-null `UserIdPrincipal`, and responds with the text `Hello, ada`, the
 * name from the credentials.
 */
fun Application.basicModule(): Unit = TODO()
