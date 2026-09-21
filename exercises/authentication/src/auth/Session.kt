package auth

import io.ktor.server.application.Application
import io.ktor.server.auth.SessionAuthenticationScheme
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.routing.Route

/**
 * Exercise 2 / 7: the login route writes the session.
 *
 * `login` registers `post("/login")` inside `authenticateWith(basicAuth) { }`:
 * the Basic scheme checks the credentials, the handler stores a [UserInfo]
 * with `call.principal.name` and the `timezone` query parameter (`UTC` when
 * absent) with `sessionAuth.setSession(…)`, and responds with
 * `HttpStatusCode.NoContent`.
 */
fun Route.login(): Unit = TODO()

/**
 * Exercise 3 / 7: the session is stored, the principal is used.
 *
 * `sessionAuth` is a `session<UserInfo, UserIdPrincipal>("auth-session")`
 * scheme: `validate { UserIdPrincipal(it.name) }` turns the stored session
 * into the principal, and `onUnauthorized = { call.respondRedirect("/login") }`
 * sends a caller without a session to the login route. Write it as a plain
 * `val`, like [basicAuth].
 */
val sessionAuth: SessionAuthenticationScheme<UserInfo, UserIdPrincipal>
  get() = TODO()

/**
 * Exercise 3 / 7: guard with the session scheme.
 *
 * `sessionModule` installs `Resources` and `install(sessionAuth)`, which puts
 * the `Sessions` plug-in in place for the scheme's cookie. The routes are
 * [login] and, inside `authenticateWith(sessionAuth) { }`, `get<Greeting.Hello>`,
 * which responds with `Hello, ada in CET`: `call.principal.name` and
 * `call.session.timezone`.
 */
fun Application.sessionModule(): Unit = TODO()
