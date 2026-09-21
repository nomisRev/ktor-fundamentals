---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 6</div>

# WebSockets and OpenAPI

## Two-way messages, described APIs

---

# HTTP answers, a WebSocket stays open

> One request, one response; or one connection both sides write to

| `HTTP/1.1 101 Switching Protocols` | the handshake: a `GET` with `Upgrade: websocket`     |
|------------------------------------|-------------------------------------------------------|
| `Frame.Text`, `Frame.Binary`       | messages, in either direction, at any time            |
| `Frame.Ping`, `Frame.Pong`         | keep the connection alive; Ktor answers them for you  |
| `Frame.Close`                      | a graceful end, with a code and a reason              |

<!--
HTTP is fire-and-forget: the client asks, the server answers, the
connection is done. A WebSocket starts as an HTTP request and, after the
`101`, stays open: either side sends a frame whenever it wants. Every
browser speaks it natively, no plug-in needed. The handshake, the pings,
and the close are handled by Ktor; the frames in between are ours.
-->

---

# WebSockets are a plug-in

<DrawnAnnotation text="install(WebSockets)" label="`ktor-server-websockets`; the client has a plug-in of its own" :geometry="{ label: { x: 0.7, y: 0.242, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets

fun Application.module() {
  install(WebSockets)
  routes()
}
```

<!--
The defaults are fine for almost everyone: `pingPeriod` (off by default,
set it to keep proxies from dropping idle connections), `timeout`,
`maxFrameSize`, `masking`. `ktor-client-websockets` installs the same
plug-in on the client, `io.ktor.client.plugins.websocket.WebSockets`.
-->

---

# A handler loops over `incoming`

<DrawnAnnotation text="webSocket(&quot;/greet&quot;)" label="A `GET /greet` that upgrades: the block runs for the whole connection" :geometry="{ label: { x: 0.72, y: 0.29, width: 0.4 } }" />
<DrawnAnnotation text="for (frame in incoming)" label="`incoming` is a channel of frames; the loop ends when the socket closes" :geometry="{ label: { x: 0.74, y: 0.4, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send

fun Application.routes() {
  routing {
    webSocket("/greet") {
      for (frame in incoming) {
        when (frame) {
          is Frame.Text -> send("Hello, ${frame.readText()}")
          else -> {}
        }
      }
    }
  }
}
```

---
magic-move
---

# A handler loops over `incoming`

<DrawnAnnotation text="is Frame.Text" label="Frames are types: text here; binary, ping, and close take other branches" :geometry="{ label: { x: 0.74, y: 0.62, width: 0.42 } }" />
<DrawnAnnotation text="send(" label="`send` writes one text frame to `outgoing`" :geometry="{ label: { x: 0.74, y: 0.72, width: 0.36 } }" />

<TypeHint :line="3" receiver="DefaultWebSocketServerSession">
<SmartCast :line="6" text="frame">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send

fun Application.routes() {
  routing {
    webSocket("/greet") {
      for (frame in incoming) {
        when (frame) {
          is Frame.Text -> send("Hello, ${frame.readText()}")
          else -> {}
        }
      }
    }
  }
}
```

</SmartCast>
</TypeHint>

<!--
`webSocket` is a route like `get`, but its block is a
`DefaultWebSocketServerSession`: `incoming` and `outgoing` are channels of
`Frame`, `send` is a shortcut for `outgoing.send(Frame.Text(…))`. The
`for` loop suspends between frames and ends when the channel closes, so the
handler returns when the client hangs up. `Frame.Binary` carries bytes,
`readBytes()`; ping, pong, and close frames are handled by the plug-in
before they reach us, which is why `else` is empty.
-->

---

# IntelliJ's HTTP client speaks WebSockets

> Any `.http` file in IntelliJ IDEA; the reply appears in the Services tool window

<DrawnAnnotation text="WEBSOCKET ws://localhost:8080/greet" label="Not `GET`: the client performs the upgrade and keeps the connection" :geometry="{ label: { x: 0.74, y: 0.37, width: 0.4 } }" />
<DrawnAnnotation text="=== wait-for-server" label="Each message is one frame; wait for the reply before sending the next" :geometry="{ label: { x: 0.72, y: 0.5, width: 0.4 } }" />

```http
WEBSOCKET ws://localhost:8080/greet

Alex
=== wait-for-server
Kodee
```

<!--
There are many clients: the browser console with `new WebSocket("ws://…")`,
`wscat`, Postman. IntelliJ IDEA's HTTP client, despite its name, does
WebSockets too: the same `.http` files as lesson 2, `WEBSOCKET` instead of
a method, one message per block. `Hello, Alex` and `Hello, Kodee` come
back in the response panel.
-->

---

# JSON frames need a converter

<DrawnAnnotation text="Response(val greeting: String)" label="The `@Serializable` classes of lesson 2, one per frame" :geometry="{ label: { x: 0.82, y: 0.31, width: 0.24 } }" />
<DrawnAnnotation text="contentConverter" label="Not `ContentNegotiation`: a frame has no `Content-Type`, you pick the format" :geometry="{ label: { x: 0.7, y: 0.66, width: 0.44 } }" />

```kotlin
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class Request(val name: String)
@Serializable data class Response(val greeting: String)

fun Application.module() {
  install(WebSockets) {
    contentConverter = KotlinxWebsocketSerializationConverter(Json)
  }
  routes()
}
```

<!--
Most message exchanges are JSON. There is no negotiation on a socket: the
frame is text or binary and nothing says which format is inside, so the
converter is fixed at install time. `KotlinxWebsocketSerializationConverter`
comes with `ktor-serialization-kotlinx-json`, the artifact lesson 2 already
added; the `Json` instance can be the same configured one.
-->

---

# Typed frames are read one at a time

<DrawnAnnotation text="receiveDeserialized<Request>()" label="Suspends until the next frame and decodes it; `while (true)` is the loop now" :geometry="{ label: { x: 0.74, y: 0.25, width: 0.4 } }" />
<DrawnAnnotation text="sendSerialized(" label="Encodes one object into one text frame" :geometry="{ label: { x: 0.74, y: 0.49, width: 0.4 } }" />

```kotlin
import io.ktor.server.routing.Route
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket

fun Route.greet() {
  webSocket("/greet") {
    while (true) {
      val req = receiveDeserialized<Request>()
      sendSerialized(Response("Hello, ${req.name}"))
    }
  }
}
```

<!--
`receiveDeserialized` and `sendSerialized` use the converter from the
plug-in; a frame that does not parse throws `WebsocketDeserializeException`
with the offending frame attached. The `for` over `incoming` is gone, so
the loop has to be explicit, and so does its end: the next two slides.
`routing { greet() }` registers this route.
-->

---
magic-move
---

# Typed frames are read one at a time

<DrawnAnnotation text="ClosedReceiveChannelException" label="The client closed the socket: `incoming` ends and the read throws" :geometry="{ label: { x: 0.72, y: 0.66, width: 0.4 } }" />

```kotlin
import io.ktor.server.routing.Route
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException

fun Route.greet() {
  webSocket("/greet") {
    try {
      while (true) {
        val req = receiveDeserialized<Request>()
        sendSerialized(Response("Hello, ${req.name}"))
      }
    } catch (e: ClosedReceiveChannelException) {
      // the client closed the socket
    }
  }
}
```

<!--
`incoming` is a `ReceiveChannel`; reading from a closed channel throws
`ClosedReceiveChannelException`. That is the normal end of a conversation,
not an error, so the catch is empty: the handler returns and Ktor finishes
the close handshake.
-->

---
magic-move
---

# Typed frames are read one at a time

<DrawnAnnotation text="CancellationException" label="The server is shutting down: send a close frame with a reason, then let it through" :geometry="{ label: { x: 0.7, y: 0.8, width: 0.44 } }" />
<DrawnAnnotation text="throw e" />

```kotlin
import io.ktor.server.routing.Route
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException

fun Route.greet() {
  webSocket("/greet") {
    try {
      while (true) {
        val req = receiveDeserialized<Request>()
        sendSerialized(Response("Hello, ${req.name}"))
      }
    } catch (e: ClosedReceiveChannelException) {
      // the client closed the socket
    } catch (e: CancellationException) {
      close(CloseReason(CloseReason.Codes.GOING_AWAY, "Shutting down"))
      throw e
    }
  }
}
```

<!--
The handler is a coroutine: when the server stops, it is cancelled and the
suspended `receiveDeserialized` throws `CancellationException`. Catching
it is the moment to say goodbye properly, `GOING_AWAY` is the code for a
server leaving; `SERVICE_RESTART` and `NORMAL` exist too. Always rethrow:
swallowing a `CancellationException` keeps a dead coroutine alive. For a
guaranteed close frame wrap the `close` in `withContext(NonCancellable)`.
-->

---

# OpenAPI describes every endpoint

> One JSON document: every path, parameter, and response; clients, docs, and tests are generated from it

| `get("/greet/{name}")`              | inferred from the routing tree                      |
|-------------------------------------|-----------------------------------------------------|
| `call.respond(GreetingResponse(…))` | inferred from the handler: a `200` with that schema |
| `/** Path: name [String] … */`      | a comment the compiler extension reads              |
| `.describe { }`                     | attached at runtime; wins over the other two        |

<!--
OpenAPI, formerly Swagger, is the specification for describing REST
services; tooling exists in almost every language to generate clients,
server stubs, and documentation from it. Writing it by hand drifts from
the code. Ktor 3.5 builds it from the routing tree: what the compiler can
infer, what a comment adds, and what a `describe` block overrides, merged
in that order of precedence.
-->

---

# The compiler extension reads your routes

<DrawnAnnotation text="id(&quot;io.ktor.plugin&quot;) version &quot;3.6.0&quot;" label="The Gradle plug-in from lesson 1 ships the compiler extension" :geometry="{ label: { x: 0.76, y: 0.242, width: 0.36 } }" />
<DrawnAnnotation text="codeInferenceEnabled = true" label="Reads `call.parameters`, `call.receive<T>()`, `call.respond(…)` inside handlers" :geometry="{ label: { x: 0.74, y: 0.5, width: 0.4 } }" />
<DrawnAnnotation text="onlyCommented = false" label="Every route is in the document; `true` keeps only the commented ones" :geometry="{ label: { x: 0.74, y: 0.62, width: 0.4 } }" />

```kotlin gradle no-compile
plugins {
  id("io.ktor.plugin") version "3.6.0"
}

ktor {
  openApi {
    enabled = true
    codeInferenceEnabled = true
    onlyCommented = false
  }
}
```

<!--
The extension runs at compile time and generates Kotlin that registers
the metadata at start-up; nothing is inspected by reflection. It needs
Kotlin 2.4.0 or newer: on an older compiler the Gradle plug-in skips the
extension with a warning and the document stays empty. `enabled = true`
adds `ktor-server-routing-openapi`, the runtime metadata API, for you;
`ktor-server-openapi` and `ktor-server-swagger` serve the document.
Inference follows extracted handler functions where it can; `// ignore!`
above a route excludes it when inference gets it wrong.
-->

---

# A comment documents the route

<DrawnAnnotation text="Greet a person by name." label="The text before the first keyword is the summary; the extension turns the comment into metadata" :geometry="{ label: { x: 0.72, y: 0.27, width: 0.4 } }" />
<DrawnAnnotation text="call.parameters[&quot;name&quot;]" label="Inferred: `name` is a path parameter" :geometry="{ label: { x: 0.78, y: 0.4, width: 0.32 } }" />
<DrawnAnnotation text="call.respond(GreetingResponse(" label="Inferred: a `200` whose schema is `GreetingResponse`" :geometry="{ label: { x: 0.7, y: 0.62, width: 0.4 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.greeting() {
  /**
   * Greet a person by name.
   */
  get("/greet/{name}") {
    val name = call.parameters["name"]
      ?: return@get call.respond(HttpStatusCode.BadRequest)
    call.respond(GreetingResponse("Hello, $name"))
  }
}
```

<!--
A KDoc comment right above the route call. The structure comes from the
routing DSL: `GET`, `/greet/{name}`; the schema of the response from the
`call.respond` the compiler sees; the prose from the comment. Nothing on
this slide is Ktor-specific syntax yet, it is documentation you would have
written anyway.
-->

---
magic-move
---

# A comment documents the route

<DrawnAnnotation text="Path: name [String] the person to greet" label="Keyword, colon, name, type, description; `Query:`, `Header:`, `Body:` follow the same shape" :geometry="{ label: { x: 0.74, y: 0.385, width: 0.4 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.greeting() {
  /**
   * Greet a person by name.
   * Path: name [String] the person to greet
   */
  get("/greet/{name}") {
    val name = call.parameters["name"]
      ?: return@get call.respond(HttpStatusCode.BadRequest)
    call.respond(GreetingResponse("Hello, $name"))
  }
}
```

<!--
A keyword at the start of a line, a colon, then the value. `Path`,
`Query`, `Header`, `Cookie` describe parameters; `Body: application/json
[Type] description` the request body; `Tag`, `Deprecated`, `Description`,
`Security`, `ExternalDocs` the rest. Plural forms with bullet lists are
accepted too: `Responses:` followed by `- 200 …` lines.
-->

---
magic-move
---

# A comment documents the route

<DrawnAnnotation text="Response: 200 [GreetingResponse]" label="Status, then the body type in brackets; the `400` has no body" :geometry="{ label: { x: 0.72, y: 0.5, width: 0.4 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.greeting() {
  /**
   * Greet a person by name.
   * Path: name [String] the person to greet
   * Response: 400 The name is missing.
   * Response: 200 [GreetingResponse] The greeting.
   */
  get("/greet/{name}") {
    val name = call.parameters["name"]
      ?: return@get call.respond(HttpStatusCode.BadRequest)
    call.respond(GreetingResponse("Hello, $name"))
  }
}
```

<!--
`Response: code contentType [Type] description`; the content type is
optional and defaults to JSON. The `200` schema was already inferred from
`call.respond`, the comment adds its description; the `400` the compiler
could not know about, `return@get call.respond(HttpStatusCode.BadRequest)`
is a status without a body.
-->

---

# `describe` documents at runtime

<DrawnAnnotation text="@OptIn(ExperimentalKtorApi::class)" />
<DrawnAnnotation text=".describe {" label="Runtime beats comments beats inference: the last word on every field" :geometry="{ label: { x: 0.74, y: 0.22, width: 0.4 } }" />
<DrawnAnnotation text="query(&quot;lang&quot;)" label="Parameters, body, responses: the DSL mirrors the OpenAPI Operation object" :geometry="{ label: { x: 0.74, y: 0.5, width: 0.4 } }" />
<DrawnAnnotation text="jsonSchema<GreetingResponse>()" label="The schema from the `@Serializable` descriptor; `@JsonSchema.Description` refines it" :geometry="{ label: { x: 0.72, y: 0.76, width: 0.44 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Route.greeting(): Route =
  get("/greet/{name}") { greet() }.describe {
    summary = "Greet a person by name"
    parameters {
      query("lang") { description = "Language of the greeting" }
    }
    responses {
      HttpStatusCode.OK {
        description = "The greeting"
        schema = jsonSchema<GreetingResponse>()
      }
    }
  }
```

<!--
`ktor-server-routing-openapi`, `io.ktor.server.routing.openapi.describe`,
experimental in 3.5 so it needs the opt-in. `describe` returns the route,
so it chains after `get`; it is the tool for what the compiler cannot see:
routes built in a loop, interceptors, conditions. The same field set by
several sources resolves to the runtime value; fields nobody overrides
are kept and merged. Schemas come from the kotlinx.serialization
descriptor by default; `ReflectionJsonSchemaInference` exists for Jackson
and Gson models.
-->

---

# Two routes serve the document

<DrawnAnnotation text="openAPI(&quot;/openapi&quot;)" label="`ktor-server-openapi`: HTML documentation rendered from the document at start-up" :geometry="{ label: { x: 0.74, y: 0.25, width: 0.4 } }" />
<DrawnAnnotation text="OpenApiDocSource.Routing(" label="Assembled from the routing tree: inference, comments, and `describe` merged" :geometry="{ label: { x: 0.7, y: 0.5, width: 0.44 } }" />

```kotlin
import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    openAPI("/openapi") {
      info = OpenApiInfo("Greeting API", "1.0.0")
      source = OpenApiDocSource.Routing(ContentType.Application.Json)
    }
  }
}
```

<!--
`info` is what the compiler cannot know: title, version, servers,
security schemes. The default `source` looks for
`openapi/documentation.yaml` in the resources first and falls back to the
routing tree, so a hand-written specification still works. `openAPI` runs
`swagger-codegen` at start-up and serves the static HTML it produced;
the documented routes are registered in this same `routing` block.
Since 3.5.2 it warns at start-up that `swagger-codegen` only officially
supports OpenAPI 3.0.x while the routing source emits 3.1.1: the HTML
may be incomplete, Swagger UI is the one to rely on.
-->

---
magic-move
---

# Two routes serve the document

<DrawnAnnotation text="swaggerUI(&quot;/swagger&quot;)" label="`ktor-server-swagger`: the document as JSON at `/swagger/openapi.json`, plus the UI that reads it" :geometry="{ label: { x: 0.74, y: 0.48, width: 0.4 } }" />

```kotlin
import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    openAPI("/openapi") {
      info = OpenApiInfo("Greeting API", "1.0.0")
      source = OpenApiDocSource.Routing(ContentType.Application.Json)
    }
    swaggerUI("/swagger") {
      info = OpenApiInfo("Greeting API", "1.0.0")
      source = OpenApiDocSource.Routing(ContentType.Application.Json)
      remotePath = "openapi.json"
    }
  }
}
```

<!--
Swagger UI is the interactive one: try a route from the browser, see the
schemas. It serves the document itself under `remotePath`, which
defaults to `documentation.yaml` whatever the content type, so name it
`openapi.json` when the source is JSON. Both
plug-ins hide their own routes from the document.
-->

---
magic-move
---

# Two routes serve the document

<DrawnAnnotation text=".hide()" label="Kept out of the document; the two plug-ins hide their own routes the same way" :geometry="{ label: { x: 0.7, y: 0.82, width: 0.44 } }" />

```kotlin
import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.routes() {
  routing {
    openAPI("/openapi") {
      info = OpenApiInfo("Greeting API", "1.0.0")
      source = OpenApiDocSource.Routing(ContentType.Application.Json)
    }
    swaggerUI("/swagger") {
      info = OpenApiInfo("Greeting API", "1.0.0")
      source = OpenApiDocSource.Routing(ContentType.Application.Json)
      remotePath = "openapi.json"
    }
    get("/health") { call.respondText("ok") }.hide()
  }
}
```

<!--
Health checks, admin endpoints, metrics: public in the server, absent
from the published API. `hide` on a `route { }` hides its whole subtree.
-->

---

# The document is JSON

<DrawnAnnotation text="GET /swagger/openapi.json" label="The same document Swagger UI renders; `/openapi` is the HTML version" :geometry="{ label: { x: 0.74, y: 0.2, width: 0.4 } }" />
<DrawnAnnotation text="&quot;/greet/{name}&quot;" label="Every route, with what inference, the comment, and `describe` said about it" :geometry="{ label: { x: 0.7, y: 0.77, width: 0.44 } }" />

```http
GET /swagger/openapi.json HTTP/1.1
Host: localhost:8080

HTTP/1.1 200 OK
Content-Type: application/json

{
  "openapi": "3.1.1",
  "info": { "title": "Greeting API", "version": "1.0.0" },
  "paths": {
    "/greet/{name}": { "get": { "summary": "Greet a person by name" } }
  }
}
```

<!--
Trimmed: the real `get` object carries `parameters`, `responses` with
their schemas under `components`, tags, and whatever else was described.
OpenAPI is huge, every element can have a description, summary, example;
the `describe` DSL exposes all of it, the comment keywords the common
part. Feed this URL to any OpenAPI generator and you have a client.
-->

---

# Two-way messages, described APIs

- `webSocket("/greet") { incoming }` → one connection, many frames
- `receiveDeserialized<T>()`, `sendSerialized(…)` → typed frames
- `ClosedReceiveChannelException` → the client left; `close(…)` to leave
- `/** Path: … Response: … */` → the compiler extension reads comments
- `.describe { }`, `.hide()`, `swaggerUI` → the last word; two routes publish it

> **The specification is the routing tree, written down.**
>
> Inference, then comments, then `describe`.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Echo JSON and publish the spec

- Add a `/chat` WebSocket route that echoes a `@Serializable` message with a timestamp
- Talk to it from an `.http` file with `WEBSOCKET ws://localhost:8080/chat`
- Enable the OpenAPI extension and document the greeting routes with KDoc
- Serve Swagger UI at `/swagger`, `.hide()` the health check, and call a route from the browser
