---
layout: intro
class: section-slide
kodee: welcome
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 1</div>

# Your first Ktor server

## From `main` to a running route

---

# Ktor is a Kotlin library for HTTP servers and clients

> By JetBrains, at `ktor.io`. Multiplatform: JVM and native.

- `server` → REST services, web applications, microservices
- `client` → calling other HTTP services (lesson 5)
- `WebSockets`, `OpenAPI` → protocols on top of HTTP (lesson 6)

<!--
The whole course is about HTTP, on both sides of the wire. The first lessons
focus on the server; the client shows up in lesson 5 when we talk to other
services. Ktor leans on Kotlin: extension functions, lambdas with receivers,
coroutines. Every slide in this deck is Ktor 3.5.2.
-->

---

# A Ktor server is one expression

<DrawnAnnotation text="Netty" label="One of the engines: Netty, CIO, Jetty, Tomcat" :geometry="{ label: { x: 0.6, y: 0.29, width: 0.4 } }" />

```kotlin
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    routing {
      get("/") {
        call.respondText("Hello world!")
      }
    }
  }.start(wait = true)
}
```

<!--
`embeddedServer` builds the server, `start(wait = true)` blocks `main` until
the process is stopped. The engine is a parameter: Netty here, CIO is the
pure Kotlin one, Jetty and Tomcat exist for servlet containers.
-->

---
magic-move
---

# A Ktor server is one expression

<DrawnAnnotation text="get(&quot;/&quot;)" label="Run this handler when `/` is requested with `GET`" :geometry="{ label: { x: 0.5, y: 0.335, width: 0.45 } }" />

```kotlin
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    routing {
      get("/") {
        call.respondText("Hello world!")
      }
    }
  }.start(wait = true)
}
```

---
magic-move
---

# A Ktor server is one expression

<DrawnAnnotation text="respondText(&quot;Hello world!&quot;)" label="The string becomes the body, `200 OK` the status" />

```kotlin
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    routing {
      get("/") {
        call.respondText("Hello world!")
      }
    }
  }.start(wait = true)
}
```

---
magic-move
---

# A Ktor server is one expression

<DrawnAnnotation text="status = HttpStatusCode.OK" label="The defaults, spelled out with named arguments" :geometry="{ label: { x: 0.69, y: 0.48, width: 0.4 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    routing {
      get("/") {
        call.respondText(
          text = "Hello world!",
          status = HttpStatusCode.OK,
        )
      }
    }
  }.start(wait = true)
}
```

<!--
Optional parameters with defaults are everywhere in Ktor. Named arguments
make the call read like the HTTP message it produces: a body and a status.
-->

---

# Gradle runs it, the log says where

<DrawnAnnotation text="./gradlew run" label="Or the run button next to `main` in IntelliJ IDEA" />
<DrawnAnnotation text="Responding at http://0.0.0.0:8080" label="Open it in a browser: `Hello world!`" />

```bash
$ ./gradlew run
[main] INFO  Application - Application started in 0.354 seconds.
[main] INFO  Application - Responding at http://0.0.0.0:8080
```

<!--
The generated project applies Gradle's `application` plug-in, so `run`
starts `main`. Every IDE that understands Gradle can do the same from the
gutter. The log comes from logback, which the generator configures for us.
-->

---

# A module is an extension of `Application`

<DrawnAnnotation text="Application.module" label="One piece of the application, apart from `main`" :geometry="{ label: { x: 0.57, y: 0.195, width: 0.4 } }" />

<TypeHint :line="2" receiver="Routing">
<TypeHint :line="3" receiver="RoutingContext">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    get("/") {
      call.respondText("Hello world!")
    }
  }
}
```

</TypeHint>
</TypeHint>

<!--
Ktor calls these pieces modules. Nothing special about the name `module`;
it is the convention the generator and the configuration file use. A larger
application has several, one per concern.
-->

---
magic-move
---

# A module is an extension of `Application`

<DrawnAnnotation text="module()" label="`main` only picks the engine and starts it" on="0" :geometry="{ label: { x: 0.42, y: 0.665, width: 0.42 } }" />
<DrawnAnnotation text="Application.module" label="Useful for testing: the test host builds the same `Application`" on="1" :geometry="{ label: { x: 0.61, y: 0.195, width: 0.48 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    get("/") {
      call.respondText("Hello world!")
    }
  }
}

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    module()
  }.start(wait = true)
}
```

<!--
Lesson 7 runs `module()` inside `testApplication { }` without Netty or a
port. That only works because starting the engine and configuring the
application are two different functions.
-->

---

# The handler is typed, the route is not

> Magic strings are the next problem

<DrawnAnnotation text="&quot;/&quot;" label="Lesson 3: type-safe routes, checked by the compiler, shared with the client" color="red" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    get("/") {
      call.respondText("Hello world!")
    }
  }
}
```

<!--
With type-safe routing the route structure is a class, defined apart from
the handler. Ktor then takes care of (de)serializing path and query
parameters, and the same class describes the route on the client side.
-->

---

# The generator writes the boilerplate

> `start.ktor.io`, or *New Project* in IntelliJ IDEA Ultimate

- Gradle build with the Ktor plug-in and the chosen dependencies
- `logback.xml` for logging
- `main` and an `Application.module()` to grow

<!--
Pick the plug-ins you need on the generator and it adds the dependencies
and an `install` for each of them. We add plug-ins by hand in this lesson
so that nothing in the project is a mystery.
-->

---

# A server turns requests into responses

> Each request is independent and starts anew

<HttpExchange :show-ktor="false" />

<!--
The picture is a simplification. Between the two there are proxies, load
balancers, a CDN, TLS termination. None of that changes the contract: one
request in, one response out, and nothing remembered in between unless we
build it ourselves.
-->

---

# A method, a path, then a status

<DrawnAnnotation text="GET" label="Method: what to do. `GET`, `POST`, `PUT`, `DELETE`" :geometry="{ label: { x: 0.6, y: 0.195, width: 0.55 } }" />
<DrawnAnnotation text="/hello" label="Path, or route: which resource" :geometry="{ label: { x: 0.5, y: 0.27, width: 0.3 } }" />

```http
GET /hello HTTP/1.1
Host: example.com
Accept: text/plain
```

<DrawnAnnotation text="200 OK" label="`2xx` fine, `4xx` the client's problem, `5xx` the server's" />

```http
HTTP/1.1 200 OK
Content-Type: text/plain; charset=UTF-8
Content-Length: 12

Hello world!
```

<!--
Both messages are text: a first line, headers, an empty line, an optional
body. The request's first line carries method and path; the response's
first line carries the status code. Everything Ktor does is reading the
first kind and writing the second.
-->

---

# One protocol, three kinds of body

<DrawnAnnotation text="PUT /user" label="REST: the path is the object, the method the action" :geometry="{ label: { x: 0.5, y: 0.195, width: 0.45 } }" />

```http
PUT /user HTTP/1.1
Content-Type: application/json

{ "name": "Alex", "timezone": "CET" }
```

<DrawnAnnotation text="<h1>Alex</h1>" label="Classic web app: the server renders HTML, forms post back" :geometry="{ label: { x: 0.5, y: 0.42, width: 0.55 } }" />

```html
<h1>Alex</h1>
<p>Identifier: 12345</p>
```

<DrawnAnnotation text="&quot;id&quot;: 12345" label="Single page app: data out, JavaScript renders" :geometry="{ label: { x: 0.65, y: 0.553, width: 0.45 } }" />

```json
{ "name": "Alex", "id": 12345 }
```

<!--
RESTful services, server-rendered pages, single page applications and
GraphQL all ride the same protocol; they differ in what the body holds.
JSON is the de facto interchange format, so Ktor's serialization support
is one of the first plug-ins we install.
-->

---

# Ktor is the server side of the exchange

> Done naively, every handler repeats itself:
>
> matching routes, checking authentication, converting JSON

<HttpExchange />

<!--
Ktor's answer to that repetition is a pipeline: the request passes through
a series of plug-ins before it reaches the handler, and the response passes
through them on the way out.
-->

---

# Plug-ins refine the request and the response

<KtorPipeline />

<!--
Routing is also a plug-in, albeit a special one: it decides which handler
runs, by method and path. Everything else, compression, authentication,
content negotiation, is a plug-in that touches the request, the response,
or both.
-->

---

# Plug-ins are installed by name

<DrawnAnnotation text="install(ContentNegotiation)" label="(De)serialization: the handler gets data, not bytes" />
<DrawnAnnotation text="json()" label="kotlinx.serialization, a dependency of its own" />

```kotlin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.module() {
  install(ContentNegotiation) {
    json()
  }
}
```

<!--
`install` takes the plug-in and a configuration block. Most plug-ins live
in their own artifact: `ktor-server-content-negotiation` here, plus
`ktor-serialization-kotlinx-json` for the `json()` format.
-->

---
magic-move
---

# Plug-ins are installed by name

<DrawnAnnotation text="install(Compression)" label="No block needed: the defaults apply" />

```kotlin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.module() {
  install(ContentNegotiation) {
    json()
  }
  install(Compression)
}
```

---
magic-move
---

# Plug-ins are installed by name

<DrawnAnnotation text="routing" label="Which handler runs, by method and path" :geometry="{ label: { x: 0.4, y: 0.48, width: 0.4 } }" />

```kotlin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  install(ContentNegotiation) {
    json()
  }
  install(Compression)

  routing {
    get("/") {
      call.respondText("Hello world!")
    }
  }
}
```

---
magic-move
---

# Plug-ins are installed by name

<DrawnAnnotation text="install(RoutingRoot)" label="`routing { }` is shorthand: Routing is a plug-in too" :geometry="{ label: { x: 0.58, y: 0.48, width: 0.48 } }" />

```kotlin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.get

fun Application.module() {
  install(ContentNegotiation) {
    json()
  }
  install(Compression)

  install(RoutingRoot) {
    get("/") {
      call.respondText("Hello world!")
    }
  }
}
```

<!--
`routing { }` installs `RoutingRoot` the first time and reuses it after
that, so several modules can each add their routes. Two plug-ins are in
almost every Ktor server: `RoutingRoot` decides which handler runs,
`ContentNegotiation` gives that handler data to work with.
-->

---

# Ktor turns requests into responses

- `embeddedServer(Netty) { … }.start(wait = true)` → the engine
- `fun Application.module()` → the application, apart from `main`
- `routing { get("/") { … } }` → one handler per method and path
- `install(Plugin) { … }` → the pipeline around the handler

> **The handler does the route-specific part.**
>
> Plug-ins refine the request before and the response after.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Build and run your first server

- Generate a project at `start.ktor.io` and open it in IntelliJ IDEA
- Run it with `./gradlew run` and open `http://0.0.0.0:8080`
- Add a `get("/hello")` route that responds with text
- Return an explicit status with `status = HttpStatusCode.Created`
- Keep the routes in `fun Application.module()`, apart from `main`
