package requests

import io.ktor.server.application.Application

/**
 * Exercise 2 / 5: receive a greeting, respond with JSON.
 *
 * `module` installs `ContentNegotiation` with `json()` and registers a
 * `post("/greet")` route that receives a `Greeting` (`call.receive`) and
 * responds with a `GreetingResponse`: `Hello, Ada` for `Type.HELLO`,
 * `Bye, Ada` for `Type.BYE`.
 *
 * Exercise 3 / 5: an optional time zone.
 *
 * When the greeting carries a time zone the message ends with it:
 * `Hello, Ada in CET`.
 *
 * Exercise 4 / 5: a blank name is a bad request.
 *
 * Respond with `HttpStatusCode.BadRequest` and no body when `name` is blank.
 *
 * Exercise 5 / 5 has no code: send `Accept: application/xml` from IntelliJ
 * IDEA's HTTP client and see what happens without `xml()`. The last test in
 * `ApplicationTest` pins the answer.
 */
fun Application.module(): Unit = TODO()
