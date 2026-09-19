package greetings

import io.ktor.server.application.Application

/**
 * Exercise 1 / 5: your own error pages.
 *
 * `statusPages` installs `StatusPages` with two rules. `status(NotFound)`
 * answers with `respondHtml(status)` and a `body` holding an `h1` saying
 * `Keep looking somewhere else`. `exception<TooEarly>` answers with
 * `HttpStatusCode.TooEarly` (425), an `h1` saying `Come back later` and a
 * `p` with the exception's `message`.
 */
fun Application.statusPages(): Unit = TODO()
