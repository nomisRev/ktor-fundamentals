package github

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun RoutingContext.profile(github: GitHubService, name: String) =
  coroutineScope {
    val info = async { github.getUserInfo(name) }
    val repos = async { github.getUserRepos(name) }
    when (val found = info.await()) {
      null -> call.respond(HttpStatusCode.NotFound)
      else -> call.respond(Profile(found, repos.await().orEmpty()))
    }
  }
