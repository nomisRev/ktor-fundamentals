package github

import io.ktor.client.HttpClient

/** A service is an interface: the handler never sees the client. */
interface GitHubService {
  suspend fun getUserInfo(user: String): User?
  suspend fun getUserRepos(user: String): List<Repo>?
}

/**
 * Exercise 3 / 4: the implementation owns the client.
 *
 * `getUserInfo` gets `/users/$user` and returns its body when the status is
 * `OK`, `null` otherwise; `getUserRepos` does the same for
 * `/users/$user/repos`. The base URL comes from the client's
 * `defaultRequest`. `close` closes the client, so that the DI plug-in can
 * close the service when the application stops.
 */
class GitHubHttp(
  private val client: HttpClient,
) : GitHubService, AutoCloseable {
  override suspend fun getUserInfo(user: String): User? = TODO()

  override suspend fun getUserRepos(user: String): List<Repo>? = TODO()

  override fun close(): Unit = TODO()
}
