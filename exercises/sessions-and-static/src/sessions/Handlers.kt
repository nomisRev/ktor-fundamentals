package sessions

import io.ktor.server.routing.RoutingContext

/**
 * Exercise 2 / 4: count the visits.
 *
 * `hello` reads the `Visits` session (`call.sessions.get`), stores it back
 * with the count plus one (`call.sessions.set`) and responds with
 * `respondHtml`: an `h1` saying `Hello, ada` and a `p` saying `Visit 2`.
 *
 * Exercise 3 / 4: link the assets.
 *
 * The page's `head` links the stylesheet (`link(rel = "stylesheet",
 * href = "/assets/style.css")`) and its `body` shows the logo
 * (`img(src = "/assets/logo.svg", alt = "Ktor")`).
 */
suspend fun RoutingContext.hello(req: Greeting.Hello): Unit = TODO()

/**
 * Exercise 2 / 4: clear the count.
 *
 * `bye` clears the `Visits` session (`call.sessions.clear`) and responds
 * with the text `Bye, ada`.
 */
suspend fun RoutingContext.bye(req: Greeting.Bye): Unit = TODO()
