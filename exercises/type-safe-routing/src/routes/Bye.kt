package routes

import io.ktor.server.routing.RoutingContext

/**
 * Exercise 1 / 4: the property name is the key.
 *
 * `bye` reads `val name: String by call.pathParameters`
 * (`import io.ktor.server.util.getValue`): the property's name is the key,
 * and the type is never `String?`, because a capture is always present. It
 * answers with the text `Bye, ada`. It lives in its own file, like [hello];
 * `module` in `Application.kt` only wires the two up.
 */
suspend fun RoutingContext.bye(): Unit = TODO()
