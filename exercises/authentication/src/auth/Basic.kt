package auth

import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential

/**
 * Exercise 1 / 5: validation returns a principal or `null`.
 *
 * `checkCredentials` accepts any name whose password is `supersecret` and
 * returns a `UserIdPrincipal` with that name, `null` otherwise. Every
 * provider in this module validates with it.
 */
fun checkCredentials(credentials: UserPasswordCredential): UserIdPrincipal? = TODO()

/**
 * Exercise 1 / 5: guard with `basic`.
 *
 * `basicModule` installs `Authentication` with a `basic("auth")` provider
 * (`realm = "Access to greetings"`, `validate { checkCredentials(it) }`),
 * installs `Resources`, and registers `get<Greeting.Hello>` inside
 * `authenticate("auth") { }`. The handler reads `call.principal<UserIdPrincipal>()`
 * and responds with the text `Hello, ada`, the name from the credentials.
 */
fun Application.basicModule(): Unit = TODO()
