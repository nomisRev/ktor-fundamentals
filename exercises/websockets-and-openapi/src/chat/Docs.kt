package chat

import io.ktor.server.routing.Route

/**
 * Exercise 4 / 4: serve the document.
 *
 * `docs` registers `swaggerUI("/swagger")` with `info = OpenApiInfo("Greeting
 * API", "1.0.0")`, `source = OpenApiDocSource.Routing(ContentType.Application.Json)`
 * and `remotePath = "openapi.json"`, so that the JSON document is at
 * `/swagger/openapi.json` and the UI at `/swagger`. It also registers
 * `get("/health")`, which responds with the text `ok`, and hides it from the
 * document with `.hide()`.
 */
fun Route.docs(): Unit = TODO()
