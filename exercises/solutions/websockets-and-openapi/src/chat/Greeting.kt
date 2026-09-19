package chat

import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.serialization.Serializable

@Serializable
data class GreetingResponse(val message: String)

@OptIn(ExperimentalKtorApi::class)
fun Route.greeting(): Route =
  get("/greet/{name}") {
    val name = call.parameters["name"]
      ?: return@get call.respond(HttpStatusCode.BadRequest)
    val hello = if (call.request.queryParameters["lang"] == "nl") "Hallo" else "Hello"
    call.respond(GreetingResponse("$hello, $name"))
  }.describe {
    summary = "Greet a person by name"
    parameters {
      path("name") { description = "The person to greet" }
      query("lang") { description = "Language of the greeting" }
    }
    responses {
      HttpStatusCode.OK {
        description = "The greeting"
        schema = jsonSchema<GreetingResponse>()
      }
    }
  }
