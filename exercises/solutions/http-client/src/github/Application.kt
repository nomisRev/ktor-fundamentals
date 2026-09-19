package github

import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module(client: HttpClient = client()) {
  dependencies {
    provide<GitHubService> { GitHubHttp(client) }
  }
  routes()
}

fun Application.routes() {
  install(ContentNegotiation) { json() }
  val github: GitHubService by dependencies
  routing {
    get("/profile/{name}") {
      val name = call.parameters["name"]
        ?: return@get call.respond(HttpStatusCode.BadRequest)
      profile(github, name)
    }
  }
}
