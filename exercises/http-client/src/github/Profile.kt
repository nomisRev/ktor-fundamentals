package github

import io.ktor.server.routing.RoutingContext

/**
 * Exercise 3 / 5: two requests, one scope.
 *
 * `profile` fetches the user info and the repositories of [name] from
 * [github] concurrently: `coroutineScope { }`, one `async { }` per call,
 * then `await` (or `awaitAll`). An unknown user (`null` info) is a
 * `404 Not Found`; otherwise respond with a [Profile] whose repositories
 * are `orEmpty()`.
 */
suspend fun RoutingContext.profile(github: GitHubService, name: String): Unit = TODO()
