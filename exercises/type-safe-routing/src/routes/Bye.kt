package routes

import io.ktor.server.routing.RoutingContext

/**
 * Exercise 4 / 4: one handler per file.
 *
 * `bye` answers with the text `Bye, ada`. It lives in its own file, like
 * [hello]; `module` in `Application.kt` only wires the two up.
 */
suspend fun RoutingContext.bye(req: Greeting.Bye): Unit = TODO()
