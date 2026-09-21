package github

import io.ktor.client.HttpClient
import io.ktor.server.application.Application

/**
 * Exercise 4 / 4: the DI plug-in provides the service.
 *
 * `module` registers `GitHubHttp(client)` as the `GitHubService` with
 * `dependencies { provide<GitHubService> { ... } }` and then calls [routes].
 * The client is a parameter so that a test can hand in one that talks to a
 * mocked GitHub.
 *
 * `routes` installs `ContentNegotiation` with `json()`, resolves the service
 * (`val github: GitHubService by dependencies`) and registers
 * `get("/profile/{name}")`, which hands the name to [profile]. A test that
 * provides a fake `GitHubService` before calling `routes()` sees the same
 * route answer without any HTTP.
 */
fun Application.module(client: HttpClient = client()): Unit = TODO()

fun Application.routes(): Unit = TODO()
