package chat

import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

/** The body the documented greeting route answers with. */
@Serializable
data class GreetingResponse(val message: String)

/**
 * Exercise 3 / 4: document the greeting route.
 *
 * `greeting` registers `get("/greet/{name}")`, which responds with a
 * [GreetingResponse] saying `Hello, ada` (`Hallo, ada` when the `lang` query
 * parameter is `nl`), and returns the route after `.describe { }`: the
 * `summary` is `Greet a person by name`, `parameters { }` documents the
 * `path("name")` and the `query("lang")` (`description = "Language of the
 * greeting"`), `responses { }` documents `HttpStatusCode.OK` with
 * `schema = jsonSchema<GreetingResponse>()`.
 *
 * The Gradle project of lesson 1 can do the same from a KDoc comment on the
 * route once the Ktor Gradle plug-in's `openApi { enabled = true }` extension
 * is on; see README.md.
 */
fun Route.greeting(): Route = TODO()
