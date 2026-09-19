package github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpStatusCode

interface GitHubService {
  suspend fun getUserInfo(user: String): User?
  suspend fun getUserRepos(user: String): List<Repo>?
}

class GitHubHttp(
  private val client: HttpClient,
) : GitHubService, AutoCloseable {
  override suspend fun getUserInfo(user: String): User? =
    client.get(GitHub.User(user))
      .takeIf { it.status == HttpStatusCode.OK }
      ?.body<User>()

  override suspend fun getUserRepos(user: String): List<Repo>? =
    client.get(GitHub.User.Repos(GitHub.User(user)))
      .takeIf { it.status == HttpStatusCode.OK }
      ?.body<List<Repo>>()

  override fun close() = client.close()
}
