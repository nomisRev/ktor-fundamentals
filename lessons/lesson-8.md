---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 8</div>

# Status pages, testing and metrics

## Failures, tests, and numbers

---

# The default error page is not yours

> An unknown route, a missing login, an exception in a handler: each one is still a response

<DrawnAnnotation text="404 Not Found" label="Ktor's default: the right status and an empty body, a blank page in the browser" :geometry="{ label: { x: 0.4871, y: 0.5305, width: 0.4400 } }" />

```http
GET /nowhere HTTP/1.1
Host: localhost:8080

HTTP/1.1 404 Not Found
Content-Length: 0
```

<!--
Three kinds of failure reach the client: no route matched, `404`; a
route refused the caller, `401` or `403`, lesson 9; a handler threw, and
Ktor answers `500` with nothing in the body. The status is right every
time, the page never is: a JSON API wants a JSON error, a website wants
its own template. One plug-in owns all three cases.
-->

---

# `StatusPages` rewrites the response

<DrawnAnnotation text="install(StatusPages)" on="0" label="`ktor-server-status-pages`: one plug-in for every failure" :geometry="{ label: { x: 0.74, y: 0.22, width: 0.4 } }" />
<DrawnAnnotation text="status(HttpStatusCode.NotFound)" label="Runs when a handler, or no handler, answers `404`; `Unauthorized` works the same" on="1" :geometry="{ label: { x: 0.7, y: 0.52, width: 0.44 } }" />
<DrawnAnnotation text="call.respondHtml(status)" on="2" />

<TypeHint :line="2" receiver="StatusPagesConfig">

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.html.body
import kotlinx.html.h1

fun Application.module() {
  install(StatusPages) {
    status(HttpStatusCode.NotFound) { call, status ->
      call.respondHtml(status) {
        body { h1 { +"Keep looking somewhere else" } }
      }
    }
  }
  routes()
}
```

</TypeHint>

<!--
The plug-in intercepts a response with that status and lets you answer
again: the same `call.respond` family as lesson 3, so JSON, HTML, or a
redirect. `status` takes several codes at once; `unhandled { }` catches a
call no route answered, which is the `404` case seen from the other
side. Keep passing the status: `respondHtml { }` alone would answer `200`
with the error page inside.
-->

---
magic-move
---

# `StatusPages` rewrites the response

<DrawnAnnotation text="statusFile(" label="Several codes, one template each, served from the classpath" :geometry="{ label: { x: 0.6427, y: 0.2839, width: 0.4000 } }" />
<DrawnAnnotation text="&quot;static/error/#.html&quot;" label="`#` becomes the code: `401.html`, `404.html`" :geometry="{ label: { x: 0.4384, y: 0.5103, width: 0.3600 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.statusFile

fun Application.module() {
  install(StatusPages) {
    statusFile(
      HttpStatusCode.Unauthorized,
      HttpStatusCode.NotFound,
      filePattern = "static/error/#.html",
    )
  }
  routes()
}
```

<!--
The files live in `src/main/resources/static/error/`, the same place
`staticResources` of lesson 5 serves from. A missing template answers
`500`, so ship one per listed code. This is the uniform version of the
previous slide: no Kotlin per page, a designer can own the HTML.
-->

---
magic-move
---

# `StatusPages` rewrites the response

<DrawnAnnotation text="exception<Throwable>" label="Any exception escaping a handler; the type parameter is the filter" :geometry="{ label: { x: 0.7231, y: 0.2637, width: 0.4000 } }" />
<DrawnAnnotation text="HttpStatusCode.InternalServerError" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.p

fun Application.module() {
  install(StatusPages) {
    exception<Throwable> { call, cause ->
      call.respondHtml(HttpStatusCode.InternalServerError) {
        body {
          h1 { +"This is embarrassing" }
          cause.message?.let { p { +it } }
        }
      }
    }
  }
  routes()
}
```

<!--
Without this, an exception is logged and the client gets an empty
`500`. With it, the handler's failure becomes a response you designed.
The most specific registered type wins when several match, so a
`Throwable` handler is the safety net under narrower ones.
-->

---
magic-move
---

# The message is not for the client

<DrawnAnnotation text="cause.message?.let { p { +it } }" label="Exfiltration: a table name, a file path, a connection string, sent to whoever asked" color="red" :geometry="{ label: { x: 0.72, y: 0.6, width: 0.44 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.p

fun Application.module() {
  install(StatusPages) {
    exception<Throwable> { call, cause ->
      call.respondHtml(HttpStatusCode.InternalServerError) {
        body {
          h1 { +"This is embarrassing" }
          cause.message?.let { p { +it } }
        }
      }
    }
  }
  routes()
}
```

---
magic-move
---

# The message is not for the client

<DrawnAnnotation text="logger.log(&quot;db: ${cause.message}&quot;)" label="The detail goes to the log, the client gets the page"  :geometry="{ label: { x: 0.5167, y: 0.5892 } }"/>

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.p

fun Application.module(logger: Logger) {
  install(StatusPages) {
    exception<Throwable> { call, cause ->
      logger.log("db: ${cause.message}")
      call.respondHtml(HttpStatusCode.InternalServerError) {
        body {
          h1 { +"This is embarrassing" }
        }
      }
    }
  }
  routes()
}
```

---
magic-move
---

# Narrow the exception, log the detail

<DrawnAnnotation text="exception<DatabaseException>" label="Only this type and its subclasses; anything else falls through to the default `500`" on="0" :geometry="{ label: { x: 0.6816, y: 0.5645, width: 0.3200 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.html.body
import kotlinx.html.h1

fun Application.module(logger: Logger) {
  install(StatusPages) {
    exception<DatabaseException> { call, cause ->
      logger.log("db: ${cause.message}")
      call.respondHtml(HttpStatusCode.InternalServerError) {
        body { h1 { +"This is embarrassing" } }
      }
    }
  }
  routes()
}
```

<!--
`exception<T>` is generic in the exception type, so one handler per
failure family: a `DatabaseException` becomes a `503` with a retry hint, a
`ValidationException` a `400` with the field names, and the `Throwable`
handler stays as the net. The `Logger` here is the deck's tiny interface;
in a real module it is `log`, the SLF4J logger every `Application` has.
Context parameters are Kotlin 2.2+, `-Xcontext-parameters`; a plain
`Application.module(logger: Logger)` says the same with more typing.
-->

---

# Validation is a plug-in

> Lesson 3 answered `400` for a blank name by hand

<DrawnAnnotation text="install(RequestValidation)" label="`ktor-server-request-validation`: runs after `ContentNegotiation` built the object" :geometry="{ label: { x: 0.6185, y: 0.3294, width: 0.5000 } }" />
<DrawnAnnotation text="validate<Greeting>" />
<DrawnAnnotation text="ValidationResult.Invalid(" />
<DrawnAnnotation text="ValidationResult.Valid" label="One rule per body type: the handler only ever sees a valid one" :geometry="{ label: { x: 0.5639, y: 0.5335, width: 0.5000 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult

fun Application.module() {
  install(RequestValidation) {
    validate<Greeting> { greeting ->
      if (greeting.name.isBlank()) ValidationResult.Invalid("name is blank")
      else ValidationResult.Valid
    }
  }
  routes()
}
```

<!--
`Greeting` is lesson 3's DTO. The rule is stated once, next to the
plug-ins, and every `call.receive<Greeting>()` in the application runs
it: a handler that gets a `Greeting` gets a valid one. The block
suspends, so a lookup is welcome; `validate { filter { }; validation { } }`
matches on anything other than the type. What happens with an invalid
body is the next slide.
-->

---
magic-move
---

# A failed validation is a `400`

<DrawnAnnotation text="install(StatusPages)" label="An exception like any other: `StatusPages` gives it its status" :geometry="{ label: { x: 0.6241, y: 0.5253, width: 0.6300 } }" />
<DrawnAnnotation text="exception<RequestValidationException>" />
<DrawnAnnotation text="cause.reasons" label="Every `Invalid` reason, collected" :geometry="{ label: { x: 0.7236, y: 0.6879, width: 0.5000 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.module() {
  install(RequestValidation) {
    validate<Greeting> { greeting ->
      if (greeting.name.isBlank()) ValidationResult.Invalid("name is blank")
      else ValidationResult.Valid
    }
  }
  install(StatusPages) {
    exception<RequestValidationException> { call, cause ->
      call.respond(HttpStatusCode.BadRequest, cause.reasons.joinToString())
    }
  }
  routes()
}
```

<!--
The plug-in throws; it does not answer. That is on purpose: the shape of
the error response is yours, and `StatusPages` from the start of this
lesson is where it is decided, once, for every route. A JSON API
responds with a `@Serializable` error object here instead of a string.
Without the handler the exception is an ordinary `500`.
-->

---

# A plug-in can be scoped to a route

<DrawnAnnotation text="route(&quot;/greet&quot;)" label="Only this subtree validates: `route { }` has its own `install`" :geometry="{ label: { x: 0.65, y: 0.289, width: 0.62 } }" />
<DrawnAnnotation text="install(RequestValidation)" label="`CallId`, `CORS`, `RateLimit`, `Authentication` install the same way" :geometry="{ label: { x: 0.75, y: 0.36, width: 0.46 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    route("/greet") {
      install(RequestValidation) {
        validate<Greeting> { greeting ->
          if (greeting.name.isBlank()) ValidationResult.Invalid("name is blank")
          else ValidationResult.Valid
        }
      }
      post {
        call.respondText("Hello, ${call.receive<Greeting>().name}")
      }
    }
  }
}
```

<!--
Most plug-ins are `RouteScopedPlugin`s: installed on the application
they apply everywhere, installed inside a `route { }` only below it. An
admin subtree with stricter validation, a public one with `CORS`, a
login route with a rate limit, lesson 9: the pipeline is per route as
much as per application. `createRouteScopedPlugin` is the
`createApplicationPlugin` of the next slide for your own.
-->

---

# A plug-in hooks into the pipeline

> When `StatusPages` is not enough, write your own

<DrawnAnnotation text="createApplicationPlugin(&quot;MyPlugin&quot;)" on="0" label="`io.ktor.server.application`: a value, installed like any plug-in" :geometry="{ label: { x: 0.6917, y: 0.4129, width: 0.2800 } }" />
<DrawnAnnotation text="onCallRespond" on="1" label="Before the body is parsed, and after `respond`: where most plug-ins live" :geometry="{ label: { x: 0.7111, y: 0.3935, width: 0.4000 } }" />
<DrawnAnnotation text="on(CallFailed)" on="2" label="Other hooks: `CallFailed`, `CallSetup`, `ResponseSent`, `MonitoringEvent(…)`" :geometry="{ label: { x: 0.7166, y: 0.4560, width: 0.4400 } }" />

```kotlin
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallFailed

val MyPlugin = createApplicationPlugin("MyPlugin") {
  onCallReceive { call -> TODO() }
  onCallRespond { call -> TODO() }
  on(CallFailed) { call, cause -> TODO() }
}
```
---

# A test runs server and client in one process

<DrawnAnnotation text="testApplication" on="0" label="`ktor-server-test-host`: starts the application inside the test, no port, no network" :geometry="{ label: { x: 0.7, y: 0.29, width: 0.44 } }" />
<DrawnAnnotation text="application { module() }" on="1" label="The module `embeddedServer` runs, unchanged" :geometry="{ label: { x: 0.76, y: 0.4, width: 0.36 } }" />
<DrawnAnnotation text="client.get(" on="2" label="A ready-made client, wired to the test engine" :geometry="{ label: { x: 0.4878, y: 0.5065, width: 0.3600 } }" />
<DrawnAnnotation text="shouldBe" on="2" />

```kotlin
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

class GreetingTest {
  @Test
  fun bye() = testApplication {
    application { module() }
    val response = client.get("/greet/alex/bye")
    response.status shouldBe HttpStatusCode.OK
  }
}
```

<!--
Testing a web server means starting it, sending requests with a client,
checking the answers. Ktor controls both ends, so `testApplication` runs
the application on a test engine and hands out a client that calls it
directly: fast, isolated, no free port needed. `application { }` is the
module, `createClient { }` builds more clients. Kotest's `shouldBe` is a
matcher; the runner is JUnit 5, `@Test` from `org.junit.jupiter.api`.
Mind the imports: there is a `get` for routes and a `get` for the
client; the IDE offers both.
-->

---

# A helper owns the set-up

<DrawnAnnotation text="createClient {" label="A client with plug-ins: JSON bodies, as in lesson 6" :geometry="{ label: { x: 0.72, y: 0.29, width: 0.44 } }" />
<DrawnAnnotation text="test(client)" label="The test receives the client and nothing else" :geometry="{ label: { x: 0.76, y: 0.48, width: 0.36 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication

fun appTest(test: suspend (HttpClient) -> Unit) = testApplication {
  application { module() }
  val client = createClient {
    install(ContentNegotiation) { json() }
  }
  test(client)
}
```

<!--
Every test starts the same application and wants the same client; the
plug-ins are the client-side ones, `io.ktor.client.plugins`, the same
artifacts as lesson 6. One function, and every test is its body.
-->

---
magic-move
---

# A helper owns the set-up

<DrawnAnnotation text="expectSuccess = true" label="Any `4xx` or `5xx` throws: the test fails without an assertion" :geometry="{ label: { x: 0.74, y: 0.43, width: 0.4 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication

fun appTest(test: suspend (HttpClient) -> Unit) = testApplication {
  application { module() }
  val client = createClient {
    install(ContentNegotiation) { json() }
    expectSuccess = true
  }
  test(client)
}
```

<!--
`expectSuccess` decides whether a failure status is a value to inspect or
an exception to raise: `ClientRequestException` for `4xx`,
`ServerResponseException` for `5xx`. For a test that expects the `404`,
build a second client without it, or check `response.status` on the
default one.
-->

---

# The test is only the test

<DrawnAnnotation text="&quot;/greet/alex/bye&quot;" label="The URL is the whole request: the helper owns everything else" :geometry="{ label: { x: 0.72, y: 0.5, width: 0.44 } }" />

```kotlin
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

@Test
fun bye() = appTest { client ->
  client.get("/greet/alex/bye").status shouldBe HttpStatusCode.OK
}
```

<!--
Still inside `GreetingTest`. No set-up in sight: the helper owns it, the
test states the request and the expectation. A renamed route shows up
here as a `404`, and `expectSuccess` turns that into a failing test.
-->

---

# The client forgets cookies unless told

> Sessions from lesson 5, logins from lesson 9: the state lives in a cookie

<DrawnAnnotation text="install(HttpCookies)" label="Keeps every `Set-Cookie` and sends it back: a login test can span requests" :geometry="{ label: { x: 0.72, y: 0.54, width: 0.44 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication

fun appTest(test: suspend (HttpClient) -> Unit) = testApplication {
  application { module() }
  val client = createClient {
    install(ContentNegotiation) { json() }
    install(HttpCookies)
    expectSuccess = true
  }
  test(client)
}
```

<!--
By default every request from the client is independent, which is what
you want until the test logs in first and then asks for a protected page.
`HttpCookies`, `io.ktor.client.plugins.cookies`, adds a cookie jar; the
`bearerAuth` and `basicAuth` request helpers cover token-based schemes.
-->

---

# Other services are mocked in the test

<DrawnAnnotation text="hosts(&quot;https://api.github.com&quot;)" label="A whole `Application` playing GitHub: routes, plug-ins, failures on demand" :geometry="{ label: { x: 0.72, y: 0.28, width: 0.44 } }" />
<DrawnAnnotation text="install(ServerContentNegotiation)" label="The server plug-in under an alias: the client's has the same name" :geometry="{ label: { x: 0.74, y: 0.53, width: 0.4 } }" />
<DrawnAnnotation text="GitHubHttp(client())" label="`HttpClient(CIO)` leaves the process: the mock never sees this request" color="red" :geometry="{ label: { x: 0.72, y: 0.79, width: 0.44 } }" />

```kotlin
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

@Test
fun profile() = testApplication {
  externalServices {
    hosts("https://api.github.com") {
      install(ServerContentNegotiation) { json() }
      routing { get("/users/{u}") { call.respond(User("Alex", null, null)) } }
    }
  }
  application { module(GitHubHttp(client())) }
  client.get("/github/alex").status shouldBe HttpStatusCode.OK
}
```

<!--
Two ways to test code that talks to another service: run the real thing
locally, Testcontainers manages the lifecycle of a database or a queue in
Docker; or mock it. `externalServices` is the mock: every host listed
gets an `Application` of its own, served by the test engine, so a `500`
or a slow answer is one route away. The catch: only a client from this
`testApplication` resolves those hosts. `GitHubHttp(client())` builds
lesson 6's real `HttpClient(CIO)`, and that one goes to the internet.
-->

---
magic-move
---

# Other services are mocked in the test

<DrawnAnnotation text="defaultRequest { url(&quot;https://api.github.com&quot;) }" label="A test client, so the mock answers; the base URL completes the relative paths" :geometry="{ label: { x: 0.8, y: 0.6, width: 0.28 } }" />
<DrawnAnnotation text="module(GitHubHttp(github))" label="The service gets the test client: lesson 6's DI, done by the test" :geometry="{ label: { x: 0.8, y: 0.76, width: 0.28 } }" />

```kotlin
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

@Test
fun profile() = testApplication {
  externalServices {
    hosts("https://api.github.com") {
      install(ServerContentNegotiation) { json() }
      routing { get("/users/{u}") { call.respond(User("Alex", null, null)) } }
    }
  }
  val github = createClient {
    install(ContentNegotiation) { json() }
    defaultRequest { url("https://api.github.com") }
  }
  application { module(GitHubHttp(github)) }
  client.get("/github/alex").status shouldBe HttpStatusCode.OK
}
```

<!--
The module takes the service as a parameter, the interface from lesson
5, so the test hands it an implementation built on a test client. The
mock answers `/users/alex`; `/users/alex/repos` has no route, `404`, and
`GitHubHttp` turns that into an empty list. Mocks are fast and can fail
on command; they are also not the real thing, so keep one integration
test against the real API, or a container, for the contract.
-->

---

# The service is tested without a server

<DrawnAnnotation text="MockEngine { request ->" label="`ktor-client-mock`: an engine that answers from a lambda, no socket" :geometry="{ label: { x: 0.74, y: 0.32, width: 0.46 } }" />
<DrawnAnnotation text="GitHubHttp(client)" label="Lesson 6's implementation, alone: no `testApplication`, no routes" :geometry="{ label: { x: 0.47, y: 0.807, width: 0.7 } }" />
<DrawnAnnotation text="runTest" label="`kotlinx-coroutines-test`: a `suspend` test body" :geometry="{ label: { x: 0.62, y: 0.242, width: 0.48 } }" />

```kotlin
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@Test
fun userInfo() = runTest {
  val engine = MockEngine { request ->
    respond(
      content = """{"name": "Alex", "bio": null, "avatar_url": null}""",
      headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
  }
  val client = HttpClient(engine) {
    install(ContentNegotiation) { json() }
    defaultRequest { url("https://api.github.com") }
  }
  GitHubHttp(client).getUserInfo("alex")?.name shouldBe "Alex"
}
```

<!--
The other unit: `externalServices` tests the route and the service
together, `MockEngine` tests the service and nothing else. The engine is
the client's lowest layer, so every plug-in above it, `ContentNegotiation`
included, runs for real; `request.url.encodedPath` in the lambda
branches on the path, `respondError(HttpStatusCode.NotFound)` plays a
missing user. Both mocks are fast and both lie a little; the contract
test against GitHub stays.
-->

---

# Metrics are a plug-in with a registry

> Which routes are hit, how long they take, how often they fail

<DrawnAnnotation text="PrometheusMeterRegistry" label="One registry per back-end: Prometheus here, JMX or Datadog are another artifact" :geometry="{ label: { x: 0.72, y: 0.4, width: 0.44 } }" />
<DrawnAnnotation text="install(MicrometerMetrics)" label="`ktor-server-metrics-micrometer`: a timer per request, tagged with route and status" :geometry="{ label: { x: 0.72, y: 0.54, width: 0.44 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun Application.module() {
  install(MicrometerMetrics) {
    registry = prometheus
  }
  routes()
}
```

<!--
A clear picture of how a service is used: when does it crash, which
endpoints carry the load, which operations are slow. Micrometer is the
facade, like SLF4J for logging: the code records timers, counters,
gauges, and distributions against a `MeterRegistry`, and the registry
implementation decides where they go. `micrometer-registry-prometheus`
1.13+ moved the classes to `io.micrometer.prometheusmetrics`. The
plug-in also registers JVM memory, GC, and CPU meters by default.
-->

---
magic-move
---

# Metrics are a plug-in with a registry

> Which routes are hit, how long they take, how often they fail

<DrawnAnnotation text="prometheus.scrape()" label="Every metric as text, in the format Prometheus reads" :geometry="{ label: { x: 0.76, y: 0.59, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun Application.module() {
  install(MicrometerMetrics) {
    registry = prometheus
  }
  routing {
    get("/metrics") { call.respond(prometheus.scrape()) }
  }
  routes()
}
```

<!--
Prometheus pulls: it does not receive metrics, it fetches them from an
endpoint on a schedule. `scrape()` renders the registry in its text
format, and a route serves it. Hide it from the OpenAPI document with
`.hide()`, and from the internet with a separate port or the auth of
lesson 9.
-->

---

# The scrape is plain text

<DrawnAnnotation text="ktor_http_server_requests_seconds_count" label="One timer per route and status: count, sum, max; Prometheus derives the rates" :geometry="{ label: { x: 0.78, y: 0.39, width: 0.32 } }" />
<DrawnAnnotation text="status=&quot;500&quot;" label="Status is a tag: errors are a query, not another metric" :geometry="{ label: { x: 0.72, y: 0.72, width: 0.44 } }" />

```http
GET /metrics HTTP/1.1
Host: localhost:8080

HTTP/1.1 200 OK
Content-Type: text/plain; charset=UTF-8

# TYPE ktor_http_server_requests_seconds summary
ktor_http_server_requests_seconds_count{route="/health",status="200"} 42
ktor_http_server_requests_seconds_sum{route="/health",status="200"} 0.213
ktor_http_server_requests_seconds_count{route="/health",status="500"} 1
```

<!--
Trimmed: every series also carries `address`, `method`, and `throwable`
tags, and the JVM meters come before it. The name is Micrometer's
`ktor.http.server.requests` with dots turned to underscores and the base
unit appended. A Prometheus query such as
`rate(ktor_http_server_requests_seconds_count[5m])` gives requests per
second; `_sum / _count` the average latency.
-->

---

# Prometheus pulls from `/metrics`

> `prometheus.yml`, next to the `prometheus` binary; the UI is on `localhost:9090`

<DrawnAnnotation text="job_name: &quot;ktor&quot;" label="The `job` label on every series from this server" :geometry="{ label: { x: 0.76, y: 0.36, width: 0.36 } }" />
<DrawnAnnotation text="targets: [&quot;localhost:8080&quot;]" label="Scraped every 15 s, at `/metrics` by default" :geometry="{ label: { x: 0.76, y: 0.5, width: 0.36 } }" />

```yaml
scrape_configs:
  - job_name: "ktor"
    static_configs:
      - targets: ["localhost:8080"]
```

<!--
Download from `prometheus.io`, unpack, add the job, run `./prometheus`.
The web UI at `localhost:9090` has the expression browser and graphs;
Grafana usually sits on top for dashboards. `metrics_path` changes the
endpoint, `scrape_interval` the cadence; in production the targets come
from service discovery rather than a static list.
-->

---

# Timers carry your tags

<DrawnAnnotation text="timers { call, throwable ->" label="Runs per request on the `Timer.Builder`: route, method, status are already there" :geometry="{ label: { x: 0.72, y: 0.39, width: 0.44 } }" />
<DrawnAnnotation text="call.request.headers[&quot;X-Premium&quot;]" label="A tag is a dimension: premium and free traffic side by side in one query" :geometry="{ label: { x: 0.72, y: 0.53, width: 0.44 } }" />

<Warning :line="6" text="throwable" message="Parameter 'throwable' is never used, could be renamed to _">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun Application.module() {
  install(MicrometerMetrics) {
    registry = prometheus
    timers { call, throwable ->
      tag("premium", call.request.headers["X-Premium"] ?: "no")
    }
  }
  routes()
}
```

</Warning>

<!--
The block sees the call and the exception, if there was one, and may add
tags to the request timer. Keep tags low-cardinality: a plan, a region, a
client version. A user id or a request id as a tag creates one series per
value and brings Prometheus to its knees; that is what logs and traces
are for.
-->

---

# Your own counters use the same registry

<DrawnAnnotation text="registry: MeterRegistry" label="The Micrometer interface, not the Prometheus class: provided by DI, swapped in tests" :geometry="{ label: { x: 0.72, y: 0.45, width: 0.44 } }" />
<DrawnAnnotation text="registry.counter(&quot;bye&quot;).increment()" label="Counters, gauges, timers, distributions: the whole Micrometer API" :geometry="{ label: { x: 0.76, y: 0.29, width: 0.36 } }" />

```kotlin
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.micrometer.core.instrument.MeterRegistry

suspend fun RoutingContext.bye(registry: MeterRegistry, name: String) {
  registry.counter("bye").increment()
  call.respondText("Bye, $name")
}
```

<!--
The request timer comes for free; business numbers are yours to record.
A counter only goes up, goodbyes said; a gauge samples a value, users
online; a timer records durations; a distribution summary any other
number. `registry.counter("bye", "lang", lang)` adds tags. Behind an
interface the metrics back-end is a detail: `SimpleMeterRegistry` in a
test, Prometheus in production, both through `ktor-server-di`.
-->

---

# Every application has a logger

<DrawnAnnotation text="log.info(" label="`Application.log`: SLF4J, behind the generator's `logback.xml`" :geometry="{ label: { x: 0.73, y: 0.242, width: 0.46 } }" />
<DrawnAnnotation text="call.application.log" label="The same logger from a handler" :geometry="{ label: { x: 0.84, y: 0.43, width: 0.32 } }" />
<DrawnAnnotation text="&quot;{} is leaving&quot;, name" label="A placeholder, not a template: formatted only when the level is on" :geometry="{ label: { x: 0.73, y: 0.5, width: 0.5 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.module() {
  log.info("Greetings module loaded")
  routing {
    get("/greet/{name}/bye") {
      val name: String by call.pathParameters
      call.application.log.warn("{} is leaving", name)
      call.respondText("Bye, $name")
    }
  }
}
```
```console
INFO  Application - Greetings module loaded
WARN  Application - alex is leaving
```

<!--
The metrics say how often; the log says what happened. `log` is the
SLF4J logger of the application, the same facade the whole JVM uses,
and logback behind it is what the generator's `logback.xml` configures:
levels per package, `io.ktor` at `INFO`, the format of a line. The
`Logger` interface earlier in this lesson stood in for this one. Log
with placeholders, never `"$name is leaving"`: the string is only built
when `WARN` is enabled, and the arguments stay separate for a JSON
encoder. `KtorSimpleLogger("name")` is the multiplatform variant.
-->

---

# `CallLogging` writes one line per request

<DrawnAnnotation text="install(CallLogging)" label="`ktor-server-call-logging`: method, path, status, duration, after the response" :geometry="{ label: { x: 0.66, y: 0.242, width: 0.6 } }" />
<DrawnAnnotation text="filter { call ->" label="Not the scrape: Prometheus every 15 seconds would drown the log" :geometry="{ label: { x: 0.55, y: 0.384, width: 0.7 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import org.slf4j.event.Level

fun Application.module() {
  install(CallLogging) {
    level = Level.INFO
    filter { call -> !call.request.path().startsWith("/metrics") }
  }
  routes()
}
```
```console
INFO  Application - 200 OK: GET - /greet/alex/bye in 3ms
INFO  Application - 404 Not Found: GET - /nowhere in 1ms
```

<!--
The access log, as a plug-in. One line per call after it completes, at
the level you choose, for the calls the filter keeps. `format { call -> }`
writes your own line; `mdc("user") { call -> … }` computes a value once
per request and puts it in the MDC, the mapped diagnostic context, so
every line logged while that request runs carries it, from any class.
That is what the next slide uses.
-->

---
magic-move
---

# `CallId` correlates the lines

<DrawnAnnotation text="header(HttpHeaders.XRequestId)" label="Reuse the caller's or the proxy's id, and echo it back" :geometry="{ label: { x: 0.72, y: 0.289, width: 0.5 } }" />
<DrawnAnnotation text="generate(length = 12)" label="Otherwise mint one" :geometry="{ label: { x: 0.52, y: 0.336, width: 0.3 } }" />
<DrawnAnnotation text="callIdMdc(&quot;call-id&quot;)" label="Into the MDC: `%X{call-id}` in `logback.xml` puts it on every line of the request" :geometry="{ label: { x: 0.66, y: 0.6, width: 0.6 } }" />

```kotlin
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import org.slf4j.event.Level

fun Application.module() {
  install(CallId) {
    header(HttpHeaders.XRequestId)
    generate(length = 12)
  }
  install(CallLogging) {
    level = Level.INFO
    filter { call -> !call.request.path().startsWith("/metrics") }
    callIdMdc("call-id")
  }
  routes()
}
```
```console
INFO  [k7d2m9x1q4z8] Application - alex is leaving
INFO  [k7d2m9x1q4z8] Application - 200 OK: GET - /greet/alex/bye in 3ms
```

<!--
A request touches several classes and, with lesson 6's client, several
services; without an id the lines of one request are scattered between
the lines of every other. `CallId` establishes one per call: taken from
`X-Request-ID` when the caller or the proxy sent it, generated
otherwise, and echoed in the response so the client can quote it. The
MDC puts it on every log line, and `ktor-client-call-id` forwards it to
the services this one calls. A metric is a tag, a log line has an id,
and the step after that, one span per call across services, is the
`ktor-server-opentelemetry` plug-in.
-->

---

# Failures, tests, and numbers

- `StatusPages { status(…), exception<T> { } }` → every failure, your page
- `RequestValidation { validate<T> { } }` → a bad body is a `400`, with reasons
- `createApplicationPlugin { transformBody { } }` → your own hook
- `testApplication { }`, `MockEngine { }` → one process, or no server at all
- `checkAll(Arb.string())`, `externalServices { }` → generated, mocked
- `MicrometerMetrics`, `CallLogging`, `CallId` → numbers, lines, an id per request

> **A failure is a response, a test is a call, a metric is a tag, a log line has an id.**
>
> Never the exception message; always the same pipeline.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Handle, test, and measure the greetings

- Map `NotFound` and your own exception to HTML pages with `StatusPages`
- Test the greeting routes with `testApplication` behind an `appTest` helper
- Add a property test with `checkAll(Arb.string())` and decide what it finds
- Expose `/metrics` with `MicrometerMetrics` and a Prometheus registry
- Scrape it with a local Prometheus and graph requests per route
