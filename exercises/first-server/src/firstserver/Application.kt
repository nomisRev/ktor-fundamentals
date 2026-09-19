package firstserver

import io.ktor.server.application.Application

// The first two bullets of the exercise slide are not code: generate a project
// at start.ktor.io, open it in IntelliJ IDEA, run it with `./gradlew run` and
// open http://0.0.0.0:8080. See README.md in this module.

/**
 * Exercise 1 / 3: answer on `/hello`.
 *
 * `module` registers a `get("/hello")` route inside `routing { }` that
 * responds with the text `Hello, world!` (`call.respondText`).
 *
 * Exercise 2 / 3: return an explicit status.
 *
 * Add a `post("/hello")` route that responds with the text `Created!` and
 * `status = HttpStatusCode.Created`.
 */
fun Application.module(): Unit = TODO()

/**
 * Exercise 3 / 3: keep the routes apart from `main`.
 *
 * `main` starts an `embeddedServer` on `Netty`, port `8080`, host `0.0.0.0`,
 * installs [module] and waits (`start(wait = true)`). The routes stay in
 * [module]: that is what the tests start, `main` is only the entry point.
 */
fun main(): Unit = TODO()
