package routes

import io.ktor.server.routing.RoutingContext

/**
 * Exercise 3 / 4: render the greeting as HTML.
 *
 * `hello` answers with `call.respondHtml`: a `head` with the `title`
 * `World Greeting Service` and a `body` with an `h1` saying
 * `Hello, ada, it's 9 o'clock` (`Hallo` instead of `Hello` when `lang` is
 * `nl`). The DSL escapes the name, so try a name with `<b>` in it.
 */
suspend fun RoutingContext.hello(req: Greeting.Hello): Unit = TODO()
