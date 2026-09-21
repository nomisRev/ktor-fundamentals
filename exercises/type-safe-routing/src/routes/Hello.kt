package routes

import io.ktor.server.routing.RoutingContext

/**
 * Exercise 1 / 4: Ktor converts the type for you.
 *
 * `hello` reads `val name: String by call.pathParameters`, the hour as an
 * `Int` from the same parameters (`/greet/ada/hello/nine` is a
 * `400 Bad Request`, nothing reaches the handler) and
 * `val lang: String? by call.queryParameters`: absent means `null`, and
 * `?lang=nl` greets with `Hallo` instead of `Hello`. Once exercise 3 exists,
 * the hour becomes `val hour by call.hour()`.
 *
 * Exercise 4 / 4: render the greeting as HTML.
 *
 * `hello` answers with `call.respondHtml`: a `head` with the `title`
 * `World Greeting Service` and a `body` with an `h1` saying
 * `Hello, ada, it's 9 o'clock`. The DSL escapes the name, so try a name
 * with `<b>` in it.
 */
suspend fun RoutingContext.hello(): Unit = TODO()
