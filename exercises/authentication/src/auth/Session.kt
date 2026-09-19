package auth

import io.ktor.server.application.Application
import io.ktor.server.routing.Route

/**
 * Exercise 2 / 5: the login route writes the session.
 *
 * `login` registers `post("/login")` inside `authenticate("auth") { }`: the
 * `basic` provider checks the credentials, the handler stores a
 * [UserInfo] with the principal's name and the `timezone` query parameter
 * (`UTC` when absent) with `call.sessions.set`, and responds with
 * `HttpStatusCode.NoContent`.
 */
fun Route.login(): Unit = TODO()

/**
 * Exercise 3 / 5: the session is the principal.
 *
 * `sessionModule` installs `Sessions` with `cookie<UserInfo>("user",
 * SessionStorageMemory())`, `Authentication` with the `basic("auth")`
 * provider of [basicModule] and a `session<UserInfo>("auth-session")`
 * provider (`validate { it }`, `challenge { call.respondRedirect("/login") }`),
 * and `Resources`. The routes are [login] and, inside
 * `authenticate("auth-session") { }`, `get<Greeting.Hello>`, which reads
 * `call.principal<UserInfo>()` and responds with `Hello, ada in CET`.
 */
fun Application.sessionModule(): Unit = TODO()
