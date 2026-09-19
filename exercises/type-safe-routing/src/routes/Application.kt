package routes

import io.ktor.server.application.Application

/**
 * Exercise 4 / 4: the plug-in does the matching.
 *
 * `module` installs `Resources` and registers `get<Greeting.Bye> { bye(it) }`
 * and `get<Greeting.Hello> { hello(it) }` (`io.ktor.server.resources.get`).
 * No string route is left.
 */
fun Application.module(): Unit = TODO()
