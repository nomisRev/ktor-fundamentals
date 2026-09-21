package routes

import io.ktor.server.application.Application

/**
 * Exercise 2 / 4: routes group by prefix.
 *
 * `module` registers `route("/greet/{name}") { }` with `get("bye") { bye() }`
 * and `get("hello/{hour}") { hello() }` inside it: the prefix and its `{name}`
 * capture are written once, and every route in the group reads them through
 * `call.pathParameters`.
 */
fun Application.module(): Unit = TODO()
