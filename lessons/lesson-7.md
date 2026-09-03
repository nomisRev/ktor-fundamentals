---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 7</div>

# Status pages, testing and metrics

## Failures, tests, and numbers

---

# The default error page is not yours

> An unknown route, a missing login, an exception in a handler: each one is still a response

<DrawnAnnotation text="404 Not Found" label="Ktor's default: the right status and an empty body, a blank page in the browser" :geometry="{ label: { x: 0.72, y: 0.45, width: 0.44 } }" />

```http
GET /nowhere HTTP/1.1
Host: localhost:8080

HTTP/1.1 404 Not Found
Content-Length: 0
```

<!--
Three kinds of failure reach the client: no route matched, `404`; a
route refused the caller, `401` or `403`, lesson 8; a handler threw, and
Ktor answers `500` with nothing in the body. The status is right every
time, the page never is: a JSON API wants a JSON error, a website wants
its own template. One plug-in owns all three cases.
-->

---

# `StatusPages` rewrites the response

<DrawnAnnotation text="install(StatusPages)" label="`ktor-server-status-pages`: one plug-in for every failure" :geometry="{ label: { x: 0.74, y: 0.22, width: 0.4 } }" />
<DrawnAnnotation text="status(HttpStatusCode.NotFound)" label="Runs when a handler, or no handler, answers `404`; `Unauthorized` works the same" :geometry="{ label: { x: 0.7, y: 0.52, width: 0.44 } }" />
<DrawnAnnotation text="call.respondHtml(status)" />

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

<!--
The plug-in intercepts a response with that status and lets you answer
again: the same `call.respond` family as lesson 2, so JSON, HTML, or a
redirect. `status` takes several codes at once; `unhandled { }` catches a
call no route answered, which is the `404` case seen from the other
side. Keep passing the status: `respondHtml { }` alone would answer `200`
with the error page inside.
-->

---
magic-move
---

# `StatusPages` rewrites the response

<DrawnAnnotation text="statusFile(" label="Several codes, one template each, served from the classpath" :geometry="{ label: { x: 0.74, y: 0.29, width: 0.4 } }" />
<DrawnAnnotation text="&quot;static/error/#.html&quot;" label="`#` becomes the code: `401.html`, `404.html`" :geometry="{ label: { x: 0.76, y: 0.43, width: 0.36 } }" />

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
`staticResources` of lesson 4 serves from. A missing template answers
`500`, so ship one per listed code. This is the uniform version of the
previous slide: no Kotlin per page, a designer can own the HTML.
-->

---
magic-move
---

# `StatusPages` rewrites the response

<DrawnAnnotation text="exception<Throwable>" label="Any exception escaping a handler; the type parameter is the filter" :geometry="{ label: { x: 0.74, y: 0.29, width: 0.4 } }" />
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

<!--
An exception message is written for the developer who reads the log:
`connection refused to db.internal:5432`, `relation "users" does not
exist`, `/etc/app/secrets.conf (permission denied)`. Every one of those
is reconnaissance for an attacker. The page says something went wrong;
the log, or an error tracker, gets the rest.
-->

---
magic-move
---

# Narrow the exception, log the detail

<DrawnAnnotation text="context(logger: Logger)" label="A context parameter: whoever calls `module` supplies the logger" :geometry="{ label: { x: 0.74, y: 0.2, width: 0.4 } }" />
<DrawnAnnotation text="exception<DatabaseException>" label="Only this type and its subclasses; anything else falls through to the default `500`" :geometry="{ label: { x: 0.78, y: 0.31, width: 0.32 } }" />
<DrawnAnnotation text="logger.log(&quot;db: ${cause.message}&quot;)" label="The detail goes to the log, the client gets the page" :geometry="{ label: { x: 0.76, y: 0.57, width: 0.36 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.html.body
import kotlinx.html.h1

context(logger: Logger)
fun Application.module() {
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

# A plug-in hooks into the pipeline

> When `StatusPages` is not enough, write your own

<DrawnAnnotation text="createApplicationPlugin(&quot;MyPlugin&quot;)" label="`io.ktor.server.application`: a value, installed like any plug-in" :geometry="{ label: { x: 0.77, y: 0.36, width: 0.28 } }" />
<DrawnAnnotation text="onCallRespond" label="Before the body is parsed, and after `respond`: where most plug-ins live" :geometry="{ label: { x: 0.74, y: 0.47, width: 0.4 } }" />
<DrawnAnnotation text="on(CallFailed)" label="Other hooks: `CallFailed`, `CallSetup`, `ResponseSent`, `MonitoringEvent(…)`" :geometry="{ label: { x: 0.72, y: 0.61, width: 0.44 } }" />

```kotlin
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallFailed

val MyPlugin = createApplicationPlugin("MyPlugin") {
  onCallReceive { call -> TODO() }
  onCallRespond { call -> TODO() }
  on(CallFailed) { call, cause -> TODO() }
}
```

<!--
Every request walks a pipeline of phases; a plug-in registers handlers
at points on it. `onCall` runs first, on every request; `onCallReceive`
when a handler asks for the body; `onCallRespond` when it answers. `on`
takes a hook object for everything else: `CallFailed` gets the exception,
`ResponseSent` runs after the bytes left, `MonitoringEvent(ApplicationStopped)`
is where `StatusPages`, `CORS`, and `MicrometerMetrics` are all written
with this same API; `createApplicationPlugin` also takes a configuration
class, which is what the `install(X) { … }` block fills in.
-->

---

# A marker object stands for a page

> Several templates, chosen by the handler: better than an exception per page

<DrawnAnnotation text="object SayPlease" label="No fields: the type is the whole message" :geometry="{ label: { x: 0.76, y: 0.36, width: 0.36 } }" />
<DrawnAnnotation text="call.respond(SayPlease)" label="An ordinary `respond`; without a plug-in it would go out as `{}`" :geometry="{ label: { x: 0.74, y: 0.5, width: 0.4 } }" />

```kotlin
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable

@Serializable
object SayPlease

suspend fun RoutingContext.please() {
  call.respond(SayPlease)
}
```

<!--
Throwing to render a page works, `StatusPages` catches it, but an
exception for a normal outcome is a lie in the type system and costs a
stack trace. A marker value is a plain response; the plug-in on the next
slide decides what it looks like on the wire. `@Serializable` keeps
`ContentNegotiation` happy if the plug-in is missing.
-->

---

# `transformBody` swaps the body

<DrawnAnnotation text="onCallRespond { call ->" label="Every `call.respond` passes through here, before `ContentNegotiation`" :geometry="{ label: { x: 0.72, y: 0.25, width: 0.44 } }" />

```kotlin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.server.application.createApplicationPlugin
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.html
import kotlinx.html.stream.createHTML

val FourOhFour = createApplicationPlugin("FourOhFour") {
  onCallRespond { call ->
    transformBody { body ->
      when (body) {
        is SayPlease -> TextContent(
          text = createHTML().html { body { h1 { +"Say please" } } },
          contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8),
          status = HttpStatusCode.Forbidden,
        )
        else -> body
      }
    }
  }
}
```

---
magic-move
---

# `transformBody` swaps the body

<DrawnAnnotation text="transformBody { body ->" label="`body` is whatever the handler passed to `respond`; return it, or another" :geometry="{ label: { x: 0.72, y: 0.3, width: 0.44 } }" />

```kotlin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.server.application.createApplicationPlugin
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.html
import kotlinx.html.stream.createHTML

val FourOhFour = createApplicationPlugin("FourOhFour") {
  onCallRespond { call ->
    transformBody { body ->
      when (body) {
        is SayPlease -> TextContent(
          text = createHTML().html { body { h1 { +"Say please" } } },
          contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8),
          status = HttpStatusCode.Forbidden,
        )
        else -> body
      }
    }
  }
}
```

<!--
`transformBody` is the main tool for writing Ktor plug-ins: refine what
came in, on `onCallReceive`; rewrite what goes out, on `onCallRespond`.
`ContentNegotiation` is exactly this, a transform from your data class to
JSON bytes; `Compression` a transform from bytes to smaller bytes.
-->

---
magic-move
---

# `transformBody` swaps the body

<DrawnAnnotation text="is SayPlease -> TextContent(" label="The marker becomes a `403` page: `TextContent` is what `respondHtml` builds" :geometry="{ label: { x: 0.72, y: 0.31, width: 0.44 } }" />
<DrawnAnnotation text="else -> body" label="Everything else untouched: the shape of every transform" :geometry="{ label: { x: 0.74, y: 0.62, width: 0.4 } }" />

```kotlin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.server.application.createApplicationPlugin
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.html
import kotlinx.html.stream.createHTML

val FourOhFour = createApplicationPlugin("FourOhFour") {
  onCallRespond { call ->
    transformBody { body ->
      when (body) {
        is SayPlease -> TextContent(
          text = createHTML().html { body { h1 { +"Say please" } } },
          contentType = ContentType.Text.Html.withCharset(Charsets.UTF_8),
          status = HttpStatusCode.Forbidden,
        )
        else -> body
      }
    }
  }
}
```

<!--
The pattern: a `when` over the special cases, `else -> body` at the end.
`TextContent` is an `OutgoingContent`, Ktor's representation of a
finished response, status and content type included; `respondHtml`
builds exactly this one, with `createHTML()` from `kotlinx.html.stream`.
A second marker is a second branch, a second template.
-->

---

# Installation order is execution order

<DrawnAnnotation text="install(FourOhFour)" label="First: it sees `SayPlease` before `ContentNegotiation` turns it into JSON" :geometry="{ label: { x: 0.72, y: 0.25, width: 0.44 } }" />
<DrawnAnnotation text="install(ContentNegotiation)" />

```kotlin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.module() {
  install(FourOhFour)
  install(ContentNegotiation) {
    json()
  }
  routes()
}
```

<!--
Both plug-ins hook the same transform phase, and hooks run in the order
they were installed. The other way around, `ContentNegotiation` would
serialize `SayPlease` to `{}` and `FourOhFour` would only ever see bytes.
It is just a plug-in: `install` it, and every route gets the behaviour.
-->

---

# A test runs server and client in one process

<DrawnAnnotation text="testApplication" label="`ktor-server-test-host`: starts the application inside the test, no port, no network" :geometry="{ label: { x: 0.7, y: 0.29, width: 0.44 } }" />
<DrawnAnnotation text="application { module() }" label="The module `embeddedServer` runs, unchanged" :geometry="{ label: { x: 0.76, y: 0.4, width: 0.36 } }" />
<DrawnAnnotation text="client.get(" label="A ready-made client, wired to the test engine" :geometry="{ label: { x: 0.76, y: 0.51, width: 0.36 } }" />
<DrawnAnnotation text="shouldBe" />

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
Mind the imports: there is a `get` for routes, a `get` for the client,
and one more for each with `@Resource`; the IDE offers all of them.
-->

---

# A helper owns the set-up

<DrawnAnnotation text="createClient {" label="A client with plug-ins: JSON bodies and `@Resource` URLs, as in lesson 5" :geometry="{ label: { x: 0.72, y: 0.29, width: 0.44 } }" />
<DrawnAnnotation text="test(client)" label="The test receives the client and nothing else" :geometry="{ label: { x: 0.76, y: 0.48, width: 0.36 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication

fun appTest(test: suspend (HttpClient) -> Unit) = testApplication {
  application { module() }
  val client = createClient {
    install(ContentNegotiation) { json() }
    install(Resources)
  }
  test(client)
}
```

<!--
Every test starts the same application and wants the same client; the
plug-ins are the client-side ones, `io.ktor.client.plugins`, the same
artifacts as lesson 5. One function, and every test is its body.
-->

---
magic-move
---

# A helper owns the set-up

<DrawnAnnotation text="expectSuccess = true" label="Any `4xx` or `5xx` throws: the test fails without an assertion" :geometry="{ label: { x: 0.74, y: 0.43, width: 0.4 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication

fun appTest(test: suspend (HttpClient) -> Unit) = testApplication {
  application { module() }
  val client = createClient {
    install(ContentNegotiation) { json() }
    install(Resources)
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

<DrawnAnnotation text="Greeting.Bye(&quot;alex&quot;)" label="The `@Resource` of lesson 3 builds `/greet/alex/bye`; the URL is never typed" :geometry="{ label: { x: 0.72, y: 0.5, width: 0.44 } }" />

```kotlin
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

@Test
fun bye() = appTest { client ->
  client.get(Greeting.Bye("alex")).status shouldBe HttpStatusCode.OK
}
```

<!--
Still inside `GreetingTest`. No set-up in sight: the helper owns it, the
test states the request and the expectation. The resource classes are
shared with the server, so a renamed route breaks this test at compile
time rather than with a `404`.
-->

---

# Generated input finds the corner cases

> Properties instead of examples: any name should get a goodbye

<DrawnAnnotation text="checkAll(Arb.string())" label="Kotest runs the block a thousand times: empty, long, and odd strings first" :geometry="{ label: { x: 0.72, y: 0.66, width: 0.44 } }" />
<DrawnAnnotation text="Greeting.Bye(name)" />

```kotlin
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

@Test
fun anyName() = appTest { client ->
  checkAll(Arb.string()) { name ->
    client.get(Greeting.Bye(name)).status shouldBe HttpStatusCode.OK
  }
}
```

<!--
Property-based testing states a rule over all inputs instead of one
example, generates the data, and runs the rule many times; good
frameworks try the edge cases first and shrink a failure to the smallest
input that still fails. A server does not choose its requests: strange
encodings, empty and enormous strings, surprising JSON. This test finds
one straight away: the empty name gives `/greet//bye`, a `404`, and
`expectSuccess` turns it into a failure. Decide whether that is a bug or
a constraint, `Arb.string(minSize = 1)`, and you have learned something
about your API. `kotest-property` is the artifact; `Arb` has generators
for every basic type and combinators to build your own.
-->

---

# The client forgets cookies unless told

> Sessions from lesson 4, logins from lesson 8: the state lives in a cookie

<DrawnAnnotation text="install(HttpCookies)" label="Keeps every `Set-Cookie` and sends it back: a login test can span requests" :geometry="{ label: { x: 0.72, y: 0.54, width: 0.44 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication

fun appTest(test: suspend (HttpClient) -> Unit) = testApplication {
  application { module() }
  val client = createClient {
    install(ContentNegotiation) { json() }
    install(Resources)
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
lesson 5's real `HttpClient(CIO)`, and that one goes to the internet.
-->

---
magic-move
---

# Other services are mocked in the test

<DrawnAnnotation text="defaultRequest { url(&quot;https://api.github.com&quot;) }" label="A test client, so the mock answers; the base URL makes resources absolute" :geometry="{ label: { x: 0.8, y: 0.6, width: 0.28 } }" />
<DrawnAnnotation text="module(GitHubHttp(github))" label="The service gets the test client: lesson 5's DI, done by the test" :geometry="{ label: { x: 0.8, y: 0.76, width: 0.28 } }" />

```kotlin
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
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
    install(Resources)
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
lesson 8.
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

# Failures, tests, and numbers

- `StatusPages { status(…), exception<T> { } }` → every failure, your page
- `createApplicationPlugin { transformBody { } }` → your own hook
- `testApplication { application { }; client }` → one process
- `checkAll(Arb.string())`, `externalServices { }` → generated, mocked
- `install(MicrometerMetrics)`, `/metrics` → numbers Prometheus pulls

> **A failure is a response, a test is a call, a metric is a tag.**
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
