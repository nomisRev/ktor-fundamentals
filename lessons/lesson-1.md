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

> By JetBrains, at `ktor.io`. Multiplatform

- `server` → REST services, web applications, microservices
- `client` → calling other HTTP services (lesson 6)
- `WebSockets`, `OpenAPI` → protocols on top of HTTP (lesson 7)

<!--
The whole course is about HTTP, on both sides of the wire. The first lessons
focus on the server; the client shows up in lesson 6 when we talk to other
services. Ktor leans on Kotlin: extension functions, lambdas with receivers,
coroutines. Every slide in this deck is Ktor 3.6.0. What is left out on
purpose: talking to a database is its own course, Exposed Fundamentals;
template engines get one mention in lesson 4; Kotlin/Native servers and
HTTP/2 and HTTP/3 are configuration of the engine, not of the application.
-->

---

# A Ktor server is one expression

<DrawnAnnotation text="Netty" label="One of the engines: Netty, CIO, Jetty, Tomcat":geometry="{ label: { x: 0.6904, y: 0.3378, width: 0.4000 } }" />

```kotlin
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    
  }.start(wait = true)
}
```

---
magic-move
---

# A Ktor server is one expression

<TypeHint :line="2" receiver="Application">
<DrawnAnnotation text="routing" label="Configure Routing for `this: Application`" :geometry="{ label: { x: 0.6868, y: 0.3328, width: 0.4500 } }" />

```kotlin
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    routing {
      
    }
  }.start(wait = true)
}
```

</TypeHint>

---
magic-move
---

# A Ktor server is one expression

<TypeHint :line="2" receiver="Application">
<TypeHint :line="3" receiver="Routing" >
<DrawnAnnotation text="get(&quot;/&quot;)" label="Run this handler when `/` is requested with `GET` in `this: Routing`" :geometry="{ label: { x: 0.4499, y: 0.4185, width: 0.4500 } }" />

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
        
      }
    }
  }.start(wait = true)
}
```

</TypeHint>
</TypeHint>

---
magic-move
---

# A Ktor server is one expression

<TypeHint :line="2" receiver="Application">
<TypeHint :line="3" receiver="Routing" >
<TypeHint :line="4" receiver="RoutingContext" >
<DrawnAnnotation text="respondText(&quot;Hello world!&quot;)" label="The string becomes the body, `200 OK` the status for `this: RoutingContext`"  :geometry="{ label: { x: 0.5633, y: 0.4631 } }"/>

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

</TypeHint>
</TypeHint>
</TypeHint>

---
magic-move
---

# A Ktor server is one expression

<TypeHint :line="2" receiver="Application">
<TypeHint :line="3" receiver="Routing" >
<TypeHint :line="4" receiver="RoutingContext" >
<DrawnAnnotation text="status = HttpStatusCode.OK" label="The defaults, spelled out with named arguments" :geometry="{ label: { x: 0.5508, y: 0.5748, width: 0.4000 } }" />

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

</TypeHint>
</TypeHint>
</TypeHint>

---

# Gradle runs it, the log says where

<DrawnAnnotation text="./gradlew run" label="Or the run button next to `main` in IntelliJ IDEA"  :geometry="{ label: { x: 0.4414, y: 0.1959 } }"/>
<DrawnAnnotation text="Responding at http://0.0.0.0:8080" label="Open it in a browser: `Hello world!` or API Client"  :geometry="{ label: { x: 0.6374, y: 0.3660 } }"/>

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

<DrawnAnnotation text="Application.module" label="One piece of the application, apart from `main`" :geometry="{ label: { x: 0.6073, y: 0.2249, width: 0.4000 } }" />

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
<DrawnAnnotation text="Application.module" label="Useful for testing: the test host builds the same `Application`" on="1" :geometry="{ label: { x: 0.5936, y: 0.2437, width: 0.4800 } }" />

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
Lesson 8 runs `module()` inside `testApplication { }` without Netty or a
port. That only works because starting the engine and configuring the
application are two different functions.
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

<DrawnAnnotation text="GET" label="Method: what to do. `GET`, `POST`, `PUT`, `DELETE`" :geometry="{ label: { x: 0.4873, y: 0.1995, width: 0.5500 } }" />
<DrawnAnnotation text="/hello" label="Path, or route: which resource" color="var(--fundamentals-blue)" :geometry="{ label: { x: 0.4332, y: 0.2484, width: 0.3000 } }" />

```http
GET /hello HTTP/1.1
Host: example.com
Accept: text/plain
```

<DrawnAnnotation text="200 OK" label="`2xx` fine, `4xx` the client's problem, `5xx` the server's" color="var(--fundamentals-pink)"  :geometry="{ label: { x: 0.4769, y: 0.3793 } }"/>

```http
HTTP/1.1 200 OK
Content-Type: text/plain; charset=UTF-8
Content-Length: 12

Hello world!
```

---

# One protocol, three kinds of body

<DrawnAnnotation text="PUT /user" label="REST: the path is the object, the method the action" :geometry="{ label: { x: 0.5588, y: 0.1952, width: 0.5672 } }" />

```http
PUT /user HTTP/1.1
Content-Type: application/json

{ "name": "Alex", "timezone": "CET" }
```

<DrawnAnnotation text="<h1>Alex</h1>" label="Classic web app: the server renders HTML, forms post back" color="var(--fundamentals-blue)" :geometry="{ label: { x: 0.5354, y: 0.4146, width: 0.6959 } }" />

```html
<h1>Alex</h1>
<p>Identifier: 12345</p>
```

<DrawnAnnotation text="&quot;id&quot;: 12345" label="Single page app: data out, JavaScript renders" color="var(--fundamentals-pink)" :geometry="{ label: { x: 0.6430, y: 0.5567, width: 0.4500 } }" />

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

---

# Plug-ins are installed by name

<TypeHint :line="2" receiver="ContentNegotiationConfig">
<DrawnAnnotation text="install(ContentNegotiation)" label="(De)serialization: the handler gets data, not bytes"  :geometry="{ label: { x: 0.5062, y: 0.3058 } }"/>
<DrawnAnnotation text="json()" label="kotlinx.serialization, a dependency of its own" color="var(--fundamentals-pink)" :geometry="{ label: { x: 0.4937, y: 0.3554 } }"/>

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
</TypeHint>

---
magic-move
---

# `ContentNegotiation` wraps the handler

<KtorPipeline plugin="ContentNegotiation" />

<DrawnAnnotation text="call.receive<Greeting>()" label="The plug-in before the handler: JSON body in, `Greeting` out" :geometry="{ label: { x: 0.7128, y: 0.7316, width: 0.3800 } }" />
<DrawnAnnotation text="call.respond(greeting)" label="The plug-in after the handler: `Greeting` in, JSON body out" color="var(--fundamentals-pink)" :geometry="{ label: { x: 0.2702, y: 0.8525, width: 0.3800 } }" />

```kotlin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.module() {
  install(ContentNegotiation) { json() }
  routing {
    post("/greet") {
      val greeting = call.receive<Greeting>()
      call.respond(greeting)
    }
  }
}
```

<!--
The picture from two slides ago, with one box named. `install` puts
`ContentNegotiation` on both rows: on the way in it reads `Content-Type`
and turns the JSON body into a `Greeting`, on the way out it turns the
`Greeting` we respond with back into JSON. The handler never sees bytes.
Strictly, the plug-in hooks into `receive` and `respond` rather than
running before and after the handler, which is why nothing happens until
the handler asks for the body. Lesson 3 covers `receive` and `respond`.
-->

---
magic-move
---

# Plug-ins are installed by name

<TypeHint :line="2" receiver="ContentNegotiationConfig">
<DrawnAnnotation text="install(Compression)" label="No block needed: the defaults apply"  :geometry="{ label: { x: 0.4756, y: 0.4271 } }"/>

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

</TypeHint>

---
magic-move
---

# Plug-ins are installed by name

<TypeHint :line="3" receiver="CompressionConfig">

```kotlin
import io.ktor.http.ContentType.Audio
import io.ktor.http.ContentType.Image
import io.ktor.http.ContentType.Text
import io.ktor.http.ContentType.Video
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.CompressionConfig
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.excludeContentType
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.identity
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.module() {
  install(ContentNegotiation) { json() }
  install(Compression) {
    mode = CompressionConfig.Mode.All
    maxEncodingChainLength = 2
    maxDecodedContentLength = -1

    gzip()
    deflate()
    identity()

    minimumSize(200)

    excludeContentType(Audio.Any, Video.Any, Image.Any, Text.EventStream)
  }
}
```

</TypeHint>

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
