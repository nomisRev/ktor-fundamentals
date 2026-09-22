---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 6</div>

# Talking to other services

## The client, coroutines, and services

---

# The client is the same framework

> Plug-ins, serialization, coroutines: the server's tools, pointed the other way

<DrawnAnnotation text="CIO" label="HttpEngine; Many choices depending on platform" on="0" color="var(--fundamentals-blue)"  :geometry="{ label: { x: 0.6626, y: 0.3000 } }"/>
<DrawnAnnotation text="ContentNegotiation" label="Same name as the server plug-in, different package: `io.ktor.client.plugins`" on="1" :geometry="{ label: { x: 0.7206, y: 0.4351, width: 0.3600 } }" />
<DrawnAnnotation text="ignoreUnknownKeys = true" label="GitHub sends more fields than we model" color="var(--fundamentals-pink)" on="2" :geometry="{ label: { x: 0.3168, y: 0.5439, width: 0.3600 } }" />

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

<DrawnAnnotation text="defaultRequest" label="Every request starts from the base URL; a request only adds the path" :geometry="{ label: { x: 0.3965, y: 0.6941, width: 0.4000 } }" />

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

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
  val name: String?,
  val bio: String?,
  @SerialName("avatar_url") val avatarUrl: String?,
)

@Serializable
data class Repo(
  val name: String,
  val description: String?,
  val fork: Boolean,
  @SerialName("stargazers_count") val stargazersCount: Int,
)
```

---

# A request is a verb and a URL

<DrawnAnnotation text="HttpClient" />
<DrawnAnnotation text="get(&quot;/users/$user&quot;)" label="The verb is the function, the path completes `defaultRequest`: `GET /users/alex`" :geometry="{ label: { x: 0.74, y: 0.5, width: 0.4 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.request.get

suspend fun HttpClient.github(user: String) {
  val response = get("/users/$user")
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

<DrawnAnnotation text="response.status" label="`HttpResponse`: status, headers, and a body still on the wire" color="var(--fundamentals-blue)" :geometry="{ label: { x: 0.6284, y: 0.2710, width: 0.3600 } }" />
<DrawnAnnotation text="response.body<User>()" label="Deserialized by `ContentNegotiation`, the mirror of `call.receive`" :geometry="{ label: { x: 0.5178, y: 0.4298, width: 0.4671 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

suspend fun HttpClient.github(user: String) {
  val response = get("/users/$user")
  when (response.status) {
    HttpStatusCode.OK -> response.body<User>()
    else -> TODO()
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

<DrawnAnnotation text="client.post(&quot;/repos/$owner/$repo&quot;) {" label="Same URL style, other verb; the block adds headers, query, body" :geometry="{ label: { x: 0.7408, y: 0.2670, width: 0.3600 } }" />
<DrawnAnnotation text="contentType(" />
<DrawnAnnotation text="setBody(" label="Serialized by the same `ContentNegotiation`; `contentType` says as what" :geometry="{ label: { x: 0.4475, y: 0.4093, width: 0.5468 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun HttpClient.describe(owner: String, repo: String, text: String) {
  post("/repos/$owner/$repo") {
    contentType(ContentType.Application.Json)
    setBody(mapOf("description" to text))
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
import io.ktor.client.HttpClient
import io.ktor.client.request.get

suspend fun HttpClient.github(user: String) {
  get("/users/$user")
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

<DrawnAnnotation text="File(&quot;uploads/$name.png&quot;).readBytes()" label="Blocks its thread until the disk answers" color="red" :geometry="{ label: { x: 0.6776, y: 0.4587, width: 0.4300 } }" />
<DrawnAnnotation text="withContext(Dispatchers.IO)" label="A pool for blocking; the handler suspends" :geometry="{ label: { x: 0.7310, y: 0.3608, width: 0.4300 } }" />

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

<DrawnAnnotation text="coroutineScope {" label="`async` needs a scope: nothing outlives the handler" :geometry="{ label: { x: 0.6260, y: 0.4853, width: 0.2800 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun HttpClient.github(user: String): Profile = coroutineScope {
  val info = async { get("/users/$user").body<User>() }
  val repos = async { get("/users/$user/repos").body<List<Repo>>() }
  Profile(info.await(), repos.await())
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

# Retries are a client plug-in

> The other side is down, or overloaded, or waking up

<DrawnAnnotation text="retryOnServerErrors(maxRetries = 5)" label="5xx only: a `404` will not change its mind, an exception does not count" :geometry="{ label: { x: 0.6509, y: 0.6131, width: 0.4000 } }" />
<DrawnAnnotation text="constantDelay(millis = 1000)" label="A second between attempts, plus up to a second of jitter" :geometry="{ label: { x: 0.3520, y: 0.7210, width: 0.3600 } }" />

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

<DrawnAnnotation text="exponentialDelay()" label="1 s, 2 s, 4 s, 8 s: room for a server that is waking up; honours `Retry-After`" :geometry="{ label: { x: 0.4376, y: 0.7191, width: 0.4400 } }" />

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
`RateLimit` plug-in, lesson 9.
-->

---
magic-move
---

# A time-out is a client plug-in too

> Without one, a silent server holds the request forever

<DrawnAnnotation text="install(HttpTimeout)" label="`ktor-client-core`; expiry is an exception, which `retryOnException` may retry" :geometry="{ label: { x: 0.6070, y: 0.7654, width: 0.5000 } }" />
<DrawnAnnotation text="requestTimeoutMillis = 5_000" label="Whole exchange; `connectTimeoutMillis` and `socketTimeoutMillis` for the parts" :geometry="{ label: { x: 0.4988, y: 0.8572, width: 0.8000 } }" />

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

---

# A service is an interface

> Depend on the interface, never on the HTTP client

<DrawnAnnotation text="suspend fun getUserInfo" label="`suspend` almost everywhere: the pipeline can wait on it" :geometry="{ label: { x: 0.7008, y: 0.3196, width: 0.3200 } }" />
<DrawnAnnotation text="List<Repo>?" label="`null` when there is no such user: no `HttpStatusCode` leaks out" color="var(--fundamentals-blue)" :geometry="{ label: { x: 0.7390, y: 0.5513, width: 0.3200 } }" />

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

# A service is an interface

> Depend on the interface, never on the HTTP client

```kotlin
sealed interface CreateRepoResult
data class Succces(val repo: Repo): CreateRepoResult
data object UserNotFound : CreateRepoResult
data object RepoAlreadyExists : CreateRepoResult

interface GitHubService {
  suspend fun getUserInfo(user: String): User?
  suspend fun getUserRepos(user: String): List<Repo>?
  suspend fun createRepo(user: String, repo: String): CreateRepoResult
}
```

---

# Handlers depend on the service

<DrawnAnnotation text="github: GitHubService" label="A fake in tests, `GitHubHttp` in `module()`: the handler cannot tell" :geometry="{ label: { x: 0.8, y: 0.24, width: 0.28 } }" />

<SmartCast :line="9" text="found">

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.util.getValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

fun Routing.profile(github: GitHubService) {
  get("/profile") {
    val user: String by call.queryParameters
    coroutineScope {
      val info = async { github.getUserInfo(user) }
      val repos = async { github.getUserRepos(user) }
      when (val found = info.await()) {
        null -> call.respond(HttpStatusCode.NotFound)
        else -> call.respond(Profile(found, repos.await().orEmpty()))
      }
    }
  }
}
```

</SmartCast>

<!--
Nothing in this function imports `io.ktor.client`. The test for it is a
`testApplication` with an object implementing `GitHubService` from a map;
the integration test uses `externalServices { hosts("https://api.github.com") { … } }`
to play GitHub, lesson 8.
-->

---

# The implementation owns the client

<DrawnAnnotation text="private val client: HttpClient" label="Built once, shared by every request: connection pool included" :geometry="{ label: { x: 0.4953, y: 0.4184, width: 0.3600 } }" />

```kotlin{1}
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class GitHubHttp(private val client: HttpClient) : GitHubService {
  override suspend fun getUserInfo(user: String): User? {
    val response = client.get("/users/$user")
    return if (response.status == HttpStatusCode.OK) response.body<User>()
    else null
  }

  override suspend fun getUserRepos(user: String): List<Repo>? =
    client.get("/users/$user/repos")
      .takeIf { it.status == HttpStatusCode.OK }
      ?.body<List<Repo>>()
}
```

---

# The implementation owns the client

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class GitHubHttp(private val client: HttpClient) : GitHubService {
  override suspend fun getUserInfo(user: String): User? {
    val response = client.get("/users/$user")
    return if (response.status == HttpStatusCode.OK) response.body<User>()
    else null
  }

  override suspend fun getUserRepos(user: String): List<Repo>? =
    client.get("/users/$user/repos")
      .takeIf { it.status == HttpStatusCode.OK }
      ?.body<List<Repo>>()
}
```

---

# Dependencies share the `Application` lifecycle

<DrawnAnnotation text="Application.gitHub" label="An extension on `Application`: an application-scoped dependency" :geometry="{ label: { x: 0.7, y: 0.2, width: 0.36 } }" />
<DrawnAnnotation text="monitor.subscribe(ApplicationStopped)" label="No more in-flight requests, no running coroutines: close the client" :geometry="{ label: { x: 0.74, y: 0.36, width: 0.4 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped

fun Application.client(): HttpClient {
  val client = HttpClient()
  monitor.subscribe(ApplicationStopped) { client.close() }
  return client
}
```

<!--
The by-hand version first. A dependency that holds a connection pool
lives as long as the application, so it is an extension on `Application`.
`monitor` is the application's event bus: `ApplicationStopped` fires
after the engine stopped accepting requests and every coroutine
completed, so closing here loses nothing. Same shape for a
`HikariDataSource`, a Kafka producer, any SDK that batches: flush and
close on `ApplicationStopped`.
-->

---
magic-move
---

# Wire the graph by hand

<DrawnAnnotation text="class Dependencies" label="The root of the graph: one class, every service the routes need" :geometry="{ label: { x: 0.78, y: 0.48, width: 0.36 } }" />
<DrawnAnnotation text="profile(dependencies.github)" label="Explicit and typed: maximum control" :geometry="{ label: { x: 0.7207, y: 0.8005, width: 0.3600 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.routing.routing

fun Application.client(): HttpClient {
  val client = HttpClient()
  monitor.subscribe(ApplicationStopped) { client.close() }
  return client
}

class Dependencies(val github: GitHubService)

fun Application.dependencies(): Dependencies =
  Dependencies(github = GitHubHttp(client()))

fun Application.module() {
  val dependencies = dependencies()
  routing { profile(dependencies.github) }
}
```

<!--
`Dependencies` aggregates the feature modules: here one service, in a
real service a `PostModule`, a `ProfileModule`, each hiding its own
infrastructure and exposing services, event handlers, webhook listeners.
Twenty-five features? Split into Gradle modules, or into services. This
is manual DI: no framework, nothing resolved by type, the compiler checks
the graph. The cost is the plumbing, every constructor argument passed by
hand, every `close()` subscribed by hand. Ktor's DI plug-in does both.
-->

---

# The DI plug-in provides the service

<DrawnAnnotation text="provide { HttpClient() }" label="Closed on shutdown because it is `AutoCloseable`" on="0"  :geometry="{ label: { x: 0.6105, y: 0.2872 } }"/>
<DrawnAnnotation text="provide<GitHubService>" label="Registered by its interface: `GitHubHttp` is a detail of `module()`" on="1" :geometry="{ label: { x: 0.4023, y: 0.4192, width: 0.4000 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve

fun Application.module() {
  dependencies {
    provide { HttpClient() }
    provide<GitHubService> { GitHubHttp(resolve<HttpClient>()) }
  }
}
```

<!--
`ktor-server-di`, `io.ktor.server.plugins.di.*`. The alternatives were
on the previous slide, by hand, or a framework such as Koin or Metro. Ktor's own plug-in registers by type, resolves lazily, and
owns the lifecycle: `AutoCloseable` dependencies are closed in reverse
order at shutdown, `provide<T> { … } cleanup { it.release() }` for anything
else. `provide<GitHubService>(::GitHubHttp)` takes a constructor reference
and resolves its parameters from the registry.
-->

---
magic-move
---

# The DI plug-in provides the service

<DrawnAnnotation text="suspend fun Application.module()" label="Modules may suspend: Ktor starts them in a coroutine" :geometry="{ label: { x: 0.5673, y: 0.2239, width: 0.3600 } }" />
<DrawnAnnotation text="dependencies.resolve()" label="Suspends until the provider has run: the same registry from another module" :geometry="{ label: { x: 0.5904, y: 0.7060, width: 0.4000 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.routing.routing

suspend fun Application.module() {
  dependencies {
    provide<HttpClient> { HttpClient() }
    provide<GitHubService> { GitHubHttp(resolve<HttpClient>()) }
  }
  routes()
}

suspend fun Application.routes() {
  val github: GitHubService = dependencies.resolve()
  routing { profile(github) }
}
```

<!--
`resolve()` is the suspending form: it waits for a provider that another
module registers later, so the order of modules stops mattering. A module
can also declare its needs as parameters, `fun Application.routes(github:
GitHubService)`, and Ktor fills them in when it loads the module from
configuration; lesson 2.
-->

---

# One framework on both ends of the wire

- `HttpClient(CIO) { install(…) }` → the server's plug-ins, another package
- `@Serializable` → the schema, shared in one module when both ends are yours
- `client.get("/users/$user")`, `body<T>()` → a verb, a URL, a typed response
- `coroutineScope { async { } }`, `awaitAll` → concurrent, not by accident
- `withContext(Dispatchers.IO)`, `HttpTimeout` → blocking off the engine, waiting with a limit
- `monitor.subscribe(ApplicationStopped)` → close what the application opened
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
