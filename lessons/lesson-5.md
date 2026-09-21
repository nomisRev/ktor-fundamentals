---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 5</div>

# Talking to other services

## The client, coroutines, and services

---

# The client is the same framework

> Plug-ins, serialization, coroutines: the server's tools, pointed the other way

<DrawnAnnotation text="CIO" />
<DrawnAnnotation text="ContentNegotiation" label="Same name as the server plug-in, different package: `io.ktor.client.plugins`" :geometry="{ label: { x: 0.76, y: 0.4, width: 0.36 } }" />
<DrawnAnnotation text="ignoreUnknownKeys = true" label="GitHub sends more fields than we model" :geometry="{ label: { x: 0.76, y: 0.53, width: 0.36 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun client(): HttpClient = HttpClient(CIO) {
  install(ContentNegotiation) {
    json(Json { ignoreUnknownKeys = true })
  }
}
```

<!--
`ktor-client-core` plus one engine: `CIO` here, `OkHttp` or `Android` on
Android, `Darwin` on Apple platforms, `Js` in the browser. The plug-ins are
`ktor-client-content-negotiation` and friends; the JSON
converter is the same `ktor-serialization-kotlinx-json` as on the server.
Same names, different packages: let the IDE import `io.ktor.client.plugins.*`
and not `io.ktor.server.plugins.*`, the compiler will not warn you.
-->

---
magic-move
---

# The client is the same framework

> Plug-ins, serialization, coroutines: the server's tools, pointed the other way

<DrawnAnnotation text="defaultRequest" label="Every request starts from the base URL; a request only adds the path" :geometry="{ label: { x: 0.74, y: 0.65, width: 0.4 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun client(): HttpClient = HttpClient(CIO) {
  install(ContentNegotiation) {
    json(Json { ignoreUnknownKeys = true })
  }
  defaultRequest { url("https://api.github.com") }
}
```

<!--
`defaultRequest` is also where a shared header goes: an `Authorization`
token for GitHub, a `User-Agent`, an `Accept`. Everything set here is the
starting point of every request; the request block can still override it.
-->

---

# The schema is a `@Serializable` class

<DrawnAnnotation text="@Serializable" label="The same annotation as the server's bodies: when both ends are yours, one module serves both" :geometry="{ label: { x: 0.72, y: 0.22, width: 0.4 } }" />
<DrawnAnnotation text="val avatar_url: String?" label="GitHub's names, GitHub's nullability: `@SerialName` if you prefer `avatarUrl`" :geometry="{ label: { x: 0.74, y: 0.42, width: 0.4 } }" />

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class User(
  val name: String?,
  val bio: String?,
  val avatar_url: String?,
)

@Serializable
data class Repo(
  val name: String,
  val description: String?,
  val fork: Boolean,
  val stargazers_count: Int,
)
```

<!--
`api.github.com/users/{username}` returns the profile, `/users/{username}/repos`
the repositories. When both ends are yours, the `@Serializable` DTOs live
in one Gradle module that server and client depend on: a changed field
fails compilation on both sides. Here the other end is GitHub, so the DTOs
only describe the part of the payload we read.
-->

---

# A request is a verb and a URL

<DrawnAnnotation text="client().use { client ->" label="One client per request, closed by `use`; a shared one comes with services" :geometry="{ label: { x: 0.79, y: 0.22, width: 0.3 } }" />
<DrawnAnnotation text="client.get(&quot;/users/$user&quot;)" label="The verb is the function, the path completes `defaultRequest`: `GET /users/alex`" :geometry="{ label: { x: 0.74, y: 0.5, width: 0.4 } }" />

```kotlin
import io.ktor.client.request.get
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.github(user: String) {
  client().use { client ->
    val response = client.get("/users/$user")
  }
}
```

<!--
`get` here is `io.ktor.client.request.get`: a relative URL is resolved
against `defaultRequest`, an absolute `"https://…"` goes where it says.
`HttpClient` is `Closeable`: a per-request client is the simplest thing that
works and the wrong thing to keep, every instance owns a connection pool.
-->

---
magic-move
---

# A request is a verb and a URL

<DrawnAnnotation text="response.status" label="`HttpResponse`: status, headers, and a body still on the wire" :geometry="{ label: { x: 0.76, y: 0.34, width: 0.36 } }" />
<DrawnAnnotation text="response.body<User>()" label="Deserialized by `ContentNegotiation`, the mirror of `call.receive`" :geometry="{ label: { x: 0.78, y: 0.49, width: 0.32 } }" />

```kotlin
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.github(user: String) {
  client().use { client ->
    val response = client.get("/users/$user")
    when (response.status) {
      HttpStatusCode.OK -> call.respond(response.body<User>())
      else -> call.respond(HttpStatusCode.NotFound)
    }
  }
}
```

<!--
`body<T>()` reads and converts in one go; `bodyAsText()` gives the raw
string. Both consume the body, so read it once. `response.headers`,
`response.contentType()`, `response.request` are all there for the asking.
-->

---

# The request block carries the body

<DrawnAnnotation text="client.post(&quot;/repos/$owner/$repo&quot;) {" label="Same URL style, other verb; the block adds headers, query, body" :geometry="{ label: { x: 0.76, y: 0.29, width: 0.36 } }" />
<DrawnAnnotation text="contentType(" />
<DrawnAnnotation text="setBody(" label="Serialized by the same `ContentNegotiation`; `contentType` says as what" :geometry="{ label: { x: 0.76, y: 0.45, width: 0.36 } }" />

```kotlin
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.describe(owner: String, repo: String, text: String) {
  client().use { client ->
    client.post("/repos/$owner/$repo") {
      contentType(ContentType.Application.Json)
      setBody(mapOf("description" to text))
    }
    call.respond(HttpStatusCode.NoContent)
  }
}
```

<!--
Every request function takes the same optional block: `header(…)`,
`parameter(…)`, `accept(…)`, `bearerAuth(…)`, `timeout { }`. Without
`contentType` the converter does not know it should run, and the body goes
out as text. GitHub actually wants `PATCH` for this one; `patch` exists too.
-->

---

# The client suspends, so does the handler

<DrawnAnnotation text="suspend" label="Waits for the network without holding a thread" :geometry="{ label: { x: 0.74, y: 0.27, width: 0.36 } }" />

```kotlin no-compile
public suspend inline fun HttpClient.get(
  urlString: String,
  block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse
```

<DrawnAnnotation text="suspend" label="The whole pipeline is a coroutine: no callbacks, no thread per request" :geometry="{ label: { x: 0.76, y: 0.53, width: 0.36 } }" />

```kotlin
import io.ktor.client.request.get
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.github(user: String) {
  client().use { client -> client.get("/users/$user") }
}
```

<!--
The keyword has been on every handler since lesson 1. Coroutines are
Kotlin's structured concurrency: a suspending call reads like a blocking
one, no `async`/`await` at every call site, no `flatMap` chains, no thread
management, and the same code on every platform. The engine parks the
coroutine while the bytes travel and resumes it on any free thread.
-->

---

# Blocking code goes to `Dispatchers.IO`

> Not everything suspends: JDBC, `java.io`, a legacy SDK

<DrawnAnnotation text="File(&quot;uploads/$name.png&quot;).readBytes()" label="Blocks its thread until the disk answers" color="red" :geometry="{ label: { x: 0.775, y: 0.4, width: 0.43 } }" />
<DrawnAnnotation text="withContext(Dispatchers.IO)" label="A pool for blocking; the handler suspends" :geometry="{ label: { x: 0.775, y: 0.352, width: 0.43 } }" />

```kotlin
import io.ktor.http.ContentType
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun RoutingContext.avatar(name: String) {
  val bytes = withContext(Dispatchers.IO) {
    File("uploads/$name.png").readBytes()
  }
  call.respondBytes(bytes, ContentType.Image.PNG)
}
```

<!--
The engine runs handlers on a few threads, one per core or so, which is
why a suspended handler costs nothing. A blocked thread is the opposite:
a JDBC query, a file read, a client library without a `suspend` API holds
one of those few threads until it returns, and a handful of slow calls
stall every other request. `withContext(Dispatchers.IO)` moves the call
to a pool made for blocking, 64 threads by default, and suspends the
handler in the meantime. Never `runBlocking` inside a handler; a library
with its own coroutine support, Exposed's `suspendTransaction` or an
R2DBC driver, does not need the switch.
-->

---

# Concurrent requests share a scope

<DrawnAnnotation text="coroutineScope {" label="`async` needs a scope: nothing outlives the handler" :geometry="{ label: { x: 0.8, y: 0.3, width: 0.28 } }" />
<DrawnAnnotation text="async { client.get(&quot;/users/$user&quot;) }.await()" label="Started, then awaited at once: the second request only begins after the first" color="red" :geometry="{ label: { x: 0.72, y: 0.6, width: 0.44 } }" />

```kotlin
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun RoutingContext.github(user: String) = coroutineScope {
  client().use { client ->
    val info = async { client.get("/users/$user") }.await()
    val repos = async { client.get("/users/$user/repos") }.await()
    call.respond(Profile(info.body<User>(), repos.body<List<Repo>>()))
  }
}
```

<!--
`async` starts a child coroutine and hands back a `Deferred`; `await`
suspends until it has a value. Back to back they are a sequential call with
extra steps, `withContext` would say the same thing. The scope is the point:
if the handler is cancelled, so are its children; if a child fails, the
handler fails.
-->

---
magic-move
---

# Concurrent requests share a scope

<DrawnAnnotation text="awaitAll(" label="Both in flight; waits for both, and cancels the other if one fails" :geometry="{ label: { x: 0.74, y: 0.34, width: 0.4 } }" />
<DrawnAnnotation text="async {" label="A lightweight task in the handler's scope, not a thread" :geometry="{ label: { x: 0.74, y: 0.66, width: 0.4 } }" />

```kotlin
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun RoutingContext.github(user: String) = coroutineScope {
  client().use { client ->
    val (info, repos) = awaitAll(
      async { client.get("/users/$user") },
      async { client.get("/users/$user/repos") },
    )
    call.respond(Profile(info.body<User>(), repos.body<List<Repo>>()))
  }
}
```

<!--
Two `await` calls in a row would also work, since both tasks are already
running; `awaitAll` says the intent and fails fast: the first exception
cancels the rest instead of waiting for it. A shared client serves both
requests from one connection pool.
-->

---

# Retries are a client plug-in

> The other side is down, or overloaded, or waking up

<DrawnAnnotation text="retryOnServerErrors(maxRetries = 5)" label="5xx only: a `404` will not change its mind, an exception does not count" :geometry="{ label: { x: 0.74, y: 0.64, width: 0.4 } }" />
<DrawnAnnotation text="constantDelay(millis = 1000)" label="A second between attempts, plus up to a second of jitter" :geometry="{ label: { x: 0.76, y: 0.75, width: 0.36 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun client(): HttpClient = HttpClient(CIO) {
  install(ContentNegotiation) {
    json(Json { ignoreUnknownKeys = true })
  }
  defaultRequest { url("https://api.github.com") }
  install(HttpRequestRetry) {
    retryOnServerErrors(maxRetries = 5)
    constantDelay(millis = 1000)
  }
}
```

<!--
`HttpRequestRetry` ships with `ktor-client-core`. `retryOnException` covers
connection failures, `retryOnExceptionOrServerErrors` both; `retryIf { }`
takes any predicate on request and response. Only retry what is safe to
repeat: a `GET` yes, a `POST` that creates something, think first.
-->

---
magic-move
---

# Retries are a client plug-in

> Retry is simple; circuit breakers live in libraries

<DrawnAnnotation text="exponentialDelay()" label="1 s, 2 s, 4 s, 8 s: room for a server that is waking up; honours `Retry-After`" :geometry="{ label: { x: 0.72, y: 0.75, width: 0.44 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun client(): HttpClient = HttpClient(CIO) {
  install(ContentNegotiation) {
    json(Json { ignoreUnknownKeys = true })
  }
  defaultRequest { url("https://api.github.com") }
  install(HttpRequestRetry) {
    retryOnServerErrors(maxRetries = 5)
    exponentialDelay()
  }
}
```

<!--
Exponential back-off: `base ^ (attempt - 1) * 1000 ms`, capped at a minute,
plus jitter so that a thousand clients do not retry in lockstep. Beyond
retries there are circuit breakers and bulkheads: the client stops here
on purpose. Resilience4j (with its Kotlin module) and Arrow's resilience
package provide them as `suspend`-friendly building blocks. The server
side of the same story, refusing callers who ask too often, is Ktor's own
`RateLimit` plug-in, lesson 8.
-->

---
magic-move
---

# A time-out is a client plug-in too

> Without one, a silent server holds the request forever

<DrawnAnnotation text="install(HttpTimeout)" label="`ktor-client-core`; expiry is an exception, which `retryOnException` may retry" :geometry="{ label: { x: 0.7, y: 0.728, width: 0.5 } }" />
<DrawnAnnotation text="requestTimeoutMillis = 5_000" label="Whole exchange; `connectTimeoutMillis` and `socketTimeoutMillis` for the parts" :geometry="{ label: { x: 0.55, y: 0.822, width: 0.8 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun client(): HttpClient = HttpClient(CIO) {
  install(ContentNegotiation) {
    json(Json { ignoreUnknownKeys = true })
  }
  defaultRequest { url("https://api.github.com") }
  install(HttpRequestRetry) {
    retryOnServerErrors(maxRetries = 5)
    exponentialDelay()
  }
  install(HttpTimeout) {
    requestTimeoutMillis = 5_000
  }
}
```

<!--
The engine's own defaults are generous or infinite: a GitHub that
accepts the connection and never answers keeps the handler, and the
caller waiting on it, suspended for as long as it likes. Three numbers:
`connectTimeoutMillis` to establish the connection,
`socketTimeoutMillis` between two packets, `requestTimeoutMillis` for
the whole request. A request that needs longer says so in its own block,
`client.get(url) { timeout { requestTimeoutMillis = 30_000 } }`. An
expired request throws `HttpRequestTimeoutException`, which is how the
retry plug-in sees it.
-->

---

# A service is an interface

> Depend on the interface, never on the HTTP client

<DrawnAnnotation text="suspend fun getUserInfo" label="`suspend` almost everywhere: the pipeline can wait on it" :geometry="{ label: { x: 0.78, y: 0.32, width: 0.32 } }" />
<DrawnAnnotation text="List<Repo>?" label="`null` when there is no such user: no `HttpStatusCode` leaks out" :geometry="{ label: { x: 0.78, y: 0.53, width: 0.32 } }" />

```kotlin
interface GitHubService {
  suspend fun getUserInfo(user: String): User?
  suspend fun getUserRepos(user: String): List<Repo>?
}
```

<!--
A server is not self-sufficient: databases, queues, caches, other HTTP
APIs. Each one is a service, and the handler talking to `HttpClient`
directly was tied to one implementation and impossible to test without
GitHub. Keep services focused, a catch-all `Database` service helps
nobody. When several routes need the same set, bundle them in one class:
`class UserService(github: GitHubService, cache: CacheService) :
GitHubService by github, CacheService by cache`, one dependency instead of
several.
-->

---

# Handlers depend on the service

<DrawnAnnotation text="github: GitHubService" label="A fake in tests, `GitHubHttp` in `module()`: the handler cannot tell" :geometry="{ label: { x: 0.8, y: 0.28, width: 0.28 } }" />
<DrawnAnnotation text="async { github.getUserInfo(user) }" label="Still concurrent: the interface suspends, so `async` still applies" :geometry="{ label: { x: 0.8, y: 0.4, width: 0.28 } }" />

<SmartCast :line="7" text="found">

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun RoutingContext.github(github: GitHubService, user: String) =
  coroutineScope {
    val info = async { github.getUserInfo(user) }
    val repos = async { github.getUserRepos(user) }
    when (val found = info.await()) {
      null -> call.respond(HttpStatusCode.NotFound)
      else -> call.respond(Profile(found, repos.await().orEmpty()))
    }
  }
```

</SmartCast>

<!--
Nothing in this function imports `io.ktor.client`. The test for it is a
`testApplication` with an object implementing `GitHubService` from a map;
the integration test uses `externalServices { hosts("https://api.github.com") { … } }`
to play GitHub, lesson 7.
-->

---

# The implementation owns the client

<DrawnAnnotation text="private val client: HttpClient" label="Built once, shared by every request: connection pool included" :geometry="{ label: { x: 0.74, y: 0.25, width: 0.36 } }" />
<DrawnAnnotation text="AutoCloseable" label="Whoever creates it closes it" :geometry="{ label: { x: 0.8, y: 0.4, width: 0.28 } }" />
<DrawnAnnotation text="client.close()" label="`close` releases the pool: once, not per request" :geometry="{ label: { x: 0.76, y: 0.81, width: 0.36 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class GitHubHttp(
  private val client: HttpClient,
) : GitHubService, AutoCloseable {
  override suspend fun getUserInfo(user: String): User? =
    client.get("/users/$user")
      .takeIf { it.status == HttpStatusCode.OK }
      ?.body<User>()

  override suspend fun getUserRepos(user: String): List<Repo>? =
    client.get("/users/$user/repos")
      .takeIf { it.status == HttpStatusCode.OK }
      ?.body<List<Repo>>()

  override fun close() = client.close()
}
```

<!--
Most services need to be initialised and closed: a connection pool, a
database, a file. `AutoCloseable` is the one requirement, and `use { }`
still works for a scoped instance: `GitHubHttp(client()).use { github ->
routing { … } }`. For the lifetime of the application there is a better
place to put that.
-->

---

# The DI plug-in provides the service

<DrawnAnnotation text="provide<GitHubService>" label="Registered by its interface: `GitHubHttp` is a detail of `module()`" :geometry="{ label: { x: 0.74, y: 0.35, width: 0.4 } }" />
<DrawnAnnotation text="GitHubHttp(client())" label="Closed on shutdown because it is `AutoCloseable`" :geometry="{ label: { x: 0.74, y: 0.46, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies

fun Application.module() {
  dependencies {
    provide<GitHubService> { GitHubHttp(client()) }
  }
  routes()
}
```

<!--
`ktor-server-di`, `io.ktor.server.plugins.di.*`. Alternatives: pass
everything by hand, maximum control and maximum plumbing; or a framework
such as Koin. Ktor's own plug-in registers by type, resolves lazily, and
owns the lifecycle: `AutoCloseable` dependencies are closed in reverse
order at shutdown, `provide<T> { … } cleanup { it.release() }` for anything
else. `provide<GitHubService>(::GitHubHttp)` takes a constructor reference
and resolves its parameters from the registry.
-->

---
magic-move
---

# The DI plug-in provides the service

<DrawnAnnotation text="by dependencies" label="Resolved at start-up: a missing provider fails the boot, not the first request" :geometry="{ label: { x: 0.74, y: 0.39, width: 0.4 } }" />
<DrawnAnnotation text="github(github, username)" label="The handler from before, now with a real service" :geometry="{ label: { x: 0.76, y: 0.54, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.module() {
  dependencies {
    provide<GitHubService> { GitHubHttp(client()) }
  }
  val github: GitHubService by dependencies
  routing {
    get("/github/{username}") {
      val username: String by call.pathParameters
      github(github, username)
    }
  }
}
```

<!--
Property delegation is the non-suspending form: the registry checks at
start-up that every required type has a provider, and the value is fetched
on first access. Names disambiguate several implementations of one
interface: `provide<GitHubService>("cached") { … }` and
`val github: GitHubService by dependencies.named("cached")`.
-->

---
magic-move
---

# The DI plug-in provides the service

<DrawnAnnotation text="suspend fun Application.module()" label="Modules may suspend: Ktor starts them in a coroutine" :geometry="{ label: { x: 0.76, y: 0.2, width: 0.36 } }" />
<DrawnAnnotation text="dependencies.resolve()" label="Suspends until the provider has run: the same registry from another module" :geometry="{ label: { x: 0.74, y: 0.75, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

suspend fun Application.module() {
  dependencies {
    provide<GitHubService> { GitHubHttp(client()) }
  }
  routes()
}

suspend fun Application.routes() {
  val github: GitHubService = dependencies.resolve()
  routing {
    get("/github/{username}") {
      val username: String by call.pathParameters
      github(github, username)
    }
  }
}
```

<!--
`resolve()` is the suspending form: it waits for a provider that another
module registers later, so the order of modules stops mattering. A module
can also declare its needs as parameters, `fun Application.routes(github:
GitHubService)`, and Ktor fills them in when it loads the module from
configuration; lesson 9.
-->

---

# One framework on both ends of the wire

- `HttpClient(CIO) { install(…) }` → the server's plug-ins, another package
- `@Serializable` → the schema, shared in one module when both ends are yours
- `client.get("/users/$user")`, `body<T>()` → a verb, a URL, a typed response
- `coroutineScope { async { } }`, `awaitAll` → concurrent, not by accident
- `withContext(Dispatchers.IO)`, `HttpTimeout` → blocking off the engine, waiting with a limit
- `interface`, `AutoCloseable`, `by dependencies` → hidden, closed, provided

> **Suspend all the way down.**
>
> The handler, the client, the service: one coroutine.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Call another API from a route

- Pick a public JSON API and model two of its responses as `@Serializable` DTOs
- Build one `HttpClient` with `ContentNegotiation`, `defaultRequest`, and `HttpRequestRetry`
- Answer `/profile/{name}` by fetching both URLs concurrently with `async` and `awaitAll`
- Hide the client behind an interface, implement it as `AutoCloseable`, provide it with `ktor-server-di`
- Swap in a fake implementation and check the route still answers
