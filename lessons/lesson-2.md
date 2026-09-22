---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 2</div>

# Requests and responses

## Headers, parameters, and the body

---

# Headers are a map on both sides

> Most headers belong to plug-ins: `ContentNegotiation` reads `Content-Type`,
>
> `DefaultHeaders` adds `Server` and `Date`

<DrawnAnnotation text="call.request.headers[&quot;X-Request-Id&quot;]" label="Nullable: the client may not have sent it" on="0"  :geometry="{ label: { x: 0.7757, y: 0.6499, width: 0.2930 } }"/>
<DrawnAnnotation text="call.response.header(&quot;X-Served-By&quot;, &quot;ktor&quot;)" label="Appended to the response before the body is written" on="1"  :geometry="{ label: { x: 0.7722, y: 0.6484, width: 0.2696 } }"/>

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    get("/hello") {
      val requestId: String? = call.request.headers["X-Request-Id"]
      call.response.header("X-Served-By", "ktor")
      call.respondText("Hello, request $requestId")
    }
  }
}
```

<!--
`call.request.headers` is read-only and case-insensitive; `call.response.headers`
accumulates what goes out. A handler rarely touches either: `Content-Type` and
`Content-Length` are written by the response functions, compression rewrites
`Content-Encoding`, and so on. Lesson 1 called this the pipeline. Even
`X-Request-Id` has a plug-in: lesson 7's `CallId` reads it, or mints one,
and puts it on every log line of the request.
-->

---
magic-move
---

# Headers are a map on both sides

<DrawnAnnotation text="install(DefaultHeaders)" label="Same header on every response, plus `Server` and `Date`"  :geometry="{ label: { x: 0.5661, y: 0.2743, width: 0.3207 } }"/>

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  install(DefaultHeaders) {
    header("X-Served-By", "ktor")
  }
  routing {
    get("/hello") {
      val requestId: String? = call.request.headers["X-Request-Id"]
      call.respondText("Hello, request $requestId")
    }
  }
}
```

<!--
`DefaultHeaders` lives in `ktor-server-default-headers`. The handler no longer
knows about `X-Served-By`: a header that every response carries is a plug-in
concern, not a route concern.
-->

---

# The request line is the route

<DrawnAnnotation text="GET /hello/Alex" />

```http
GET /hello/Alex?timezone=CET HTTP/1.1
Host: example.com
```

<DrawnAnnotation text="get(&quot;/hello/{name}&quot;)" label="One handler per method and path pattern" :geometry="{ label: { x: 0.5615, y: 0.4138, width: 0.4000 } }" />
<DrawnAnnotation text="{name}" label="`Alex` is captured as `name`" :geometry="{ label: { x: 0.5395, y: 0.4594, width: 0.3000 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    get("/hello/{name}") {
      call.respondText("Hello")
    }
  }
}
```

<!--
The mapping is 1:1: the method becomes the function, the path becomes its
argument. A path pattern can capture a segment with braces; `{name?}` makes it
optional and `{...}` matches the rest of the path.
-->

---
magic-move
---

# The request line is the route

```http
GET /hello/Alex?timezone=CET HTTP/1.1
Host: example.com
```

<DrawnAnnotation text="call.parameters[&quot;name&quot;]" label="Capture names the object: a `String?`, the pattern is only a string"  :geometry="{ label: { x: 0.6565, y: 0.5049, width: 0.5307 } }"/>

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    get("/hello/{name}") {
      val name = call.parameters["name"]
      call.respondText("Hello, $name")
    }
  }
}
```

<!--
`call.parameters` holds the captured segments. The value is present whenever
this handler runs, but the type system cannot know that from a string pattern,
hence the nullable type. Lesson 3 fixes that with property delegation.
-->

---
magic-move
---

# The request line is the route

<DrawnAnnotation text="timezone=CET" label="After `?`: `key=value` pairs, `&`-separated"  :geometry="{ label: { x: 0.5100, y: 0.6809 } }"/>

```http
GET /hello/Alex?timezone=CET HTTP/1.1
Host: example.com
```

<DrawnAnnotation text="call.request.queryParameters[&quot;timezone&quot;]" label="Query tweaks the request, usually optional: `null` when absent"  :geometry="{ label: { x: 0.5258, y: 0.6309 } }"/>

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    get("/hello/{name}") {
      val name = call.parameters["name"]
      val timezone = call.request.queryParameters["timezone"]
      call.respondText("Hello, $name in $timezone")
    }
  }
}
```

<!--
`call.request.queryParameters` (or the shorthand `call.queryParameters`) is a
`Parameters` map like the captures; `getAll` returns repeated keys. The route
does not mention query parameters: any `GET /hello/{name}` matches, with or
without them.
-->

---
magic-move
---

# The request line is the route

<DrawnAnnotation text="Content-Type: application/json" label="Tells the plug-in which format to parse" :geometry="{ label: { x: 0.5004, y: 0.2909, width: 0.3400 } }" />
<DrawnAnnotation text="{ &quot;type&quot;: &quot;hello&quot;, &quot;name&quot;: &quot;Alex&quot;, &quot;timezone&quot;: &quot;CET&quot; }" />

```http
POST /greet HTTP/1.1
Host: example.com
Content-Type: application/json

{ "type": "hello", "name": "Alex", "timezone": "CET" }
```

<DrawnAnnotation text="call.receive<Greeting>()" label="Body carries the object: `ContentNegotiation` builds it from the JSON"  :geometry="{ label: { x: 0.5654, y: 0.7447, width: 0.4274 } }"/>

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    post("/greet") {
      val greeting = call.receive<Greeting>()
      call.respondText("Hello, ${greeting.name}")
    }
  }
}
```

<!--
Three places for parameters, three idioms. The capture identifies which
object the request is about, typical for `GET`; the body carries the object
itself, typical for `POST` and `PUT`; query parameters tweak the request and
are usually optional. Follow the specification of the service you implement.
`Greeting` and the plug-in behind `receive` are the second half of this lesson.
-->

---

# A form is a body too

<DrawnAnnotation text="application/x-www-form-urlencoded" label="What `<form method=&quot;post&quot;>` sends: `key=value` pairs, like a query string" :geometry="{ label: { x: 0.6312, y: 0.3676, width: 0.3600 } }" />
<DrawnAnnotation text="call.receiveParameters()" label="The same `Parameters` map as the query: `[&quot;name&quot;]`, `getAll`" :geometry="{ label: { x: 0.4625, y: 0.7449, width: 0.7216 } }" />

```http
POST /greet HTTP/1.1
Host: example.com
Content-Type: application/x-www-form-urlencoded

type=hello&name=Alex&timezone=CET
```

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    post("/greet") {
      val form: Parameters = call.receiveParameters()
      call.respondText("Hello, ${form["name"]}")
    }
  }
}
```

---

# A file arrives in parts

<DrawnAnnotation text="receiveMultipart()" label="Fields and files, part by part" :geometry="{ label: { x: 0.7530, y: 0.4133, width: 0.3200 } }" />

```kotlin
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

fun Application.module() {
  routing {
    post("/avatar") {
      val multipart: MultiPartData = call.receiveMultipart()
      
      call.respondText("Uploaded")
    }
  }
}
```

---
magic-move
---

# A file arrives in parts

<DrawnAnnotation text="PartData.FileItem" label="A file; a field is a `FormItem`" :geometry="{ label: { x: 0.6790, y: 0.4838, width: 0.4000 } }" />

```kotlin
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

fun Application.module() {
  routing {
    post("/avatar") {
      val multipart: MultiPartData = call.receiveMultipart()
      multipart.asFlow()
        .filterIsInstance<PartData.FileItem>()
      
      call.respondText("Uploaded")
    }
  }
}
```

---
magic-move
---

# A file arrives in parts

<DrawnAnnotation text="copyAndClose(" label="Streamed to disk, never whole in memory" :geometry="{ label: { x: 0.6485, y: 0.6361, width: 0.4400 } }" />

```kotlin
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

fun Application.module() {
  routing {
    post("/avatar") {
      val multipart: MultiPartData = call.receiveMultipart()
      multipart.asFlow()
        .filterIsInstance<PartData.FileItem>()
        .collect { part ->
            val file = File("uploads/${part.originalFileName ?: "avatar"}")
            part.provider().copyAndClose(file.writeChannel())
        }
      call.respondText("Uploaded")
    }
  }
}
```

---
magic-move
---

# A file arrives in parts

<DrawnAnnotation text="part.release()" label="Release the part" :geometry="{ label: { x: 0.4372, y: 0.7639, width: 0.3000 } }" />

```kotlin
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

fun Application.module() {
  routing {
    post("/avatar") {
      val multipart: MultiPartData = call.receiveMultipart()
      multipart.asFlow()
        .filterIsInstance<PartData.FileItem>()
        .collect { part ->
          try {
            val file = File("uploads/${part.originalFileName ?: "avatar"}")
            part.provider().copyAndClose(file.writeChannel())
          } finally {
            part.release()
          }
        }
      call.respondText("Uploaded")
    }
  }
}
```

---
magic-move
---

# A file arrives in parts

```kotlin
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

fun Application.module() {
  routing {
    post("/avatar") {
      val multipart: MultiPartData = call.receiveMultipart()
      multipart.forEachPart { part ->
        if (part is PartData.FileItem) {
          try {
            val file = File("uploads/${part.originalFileName ?: "avatar"}")
            part.provider().copyAndClose(file.writeChannel())
          } finally {
            part.release()
          }
        }
      }
      call.respondText("Uploaded")
    }
  }
}
```


---

# A response is a status and a body

<DrawnAnnotation text="status = HttpStatusCode.Created" label="Optional: `200 OK` unless said otherwise"  :geometry="{ label: { x: 0.6655, y: 0.4546 } }"/>

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    post("/greet/{name}") {
      val name = call.parameters["name"]
      call.respondText("Hello, $name", status = HttpStatusCode.Created)
    }
  }
}
```

<DrawnAnnotation text="201 Created" label="The status text is for humans; clients read the number"  :geometry="{ label: { x: 0.5436, y: 0.6134 } }"/>

```http
HTTP/1.1 201 Created
Content-Type: text/plain; charset=UTF-8
Content-Length: 11

Hello, Alex
```

<!--
`respondText`, `respondBytes`, `respondFile`, `respondRedirect`: one function
per kind of body, all with a `status` parameter that defaults to `200 OK`.
`Content-Type` and `Content-Length` are derived from the body.
-->

---
magic-move
---

# A response is a status and a body

<DrawnAnnotation text="call.respond(HttpStatusCode.NotFound)" label="A status alone: the body stays empty"  :geometry="{ label: { x: 0.7166, y: 0.3225 } }"/>

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    post("/greet/{name}") {
      val name = call.parameters["name"]
      if (name !in users) call.respond(HttpStatusCode.NotFound)
      else call.respondText("Hello, $name", status = HttpStatusCode.Created)
    }
  }
}
```

```http
HTTP/1.1 404 Not Found
Content-Length: 0
```

<!--
`users` is a set of known names. `4xx` is the client's problem, `5xx` the
server's; an exception escaping the handler becomes a `500` unless the
`StatusPages` plug-in maps it to something better (lesson 7).
-->

---
magic-move
---

# A response is a status and a body

<DrawnAnnotation text="GreetingResponse(&quot;Hello, $name&quot;)" label="An object: `ContentNegotiation` turns it into the body" :geometry="{ label: { x: 0.7294, y: 0.6170, width: 0.4200 } }" />

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable


fun interface UserService {
  operator fun contains(name: String?): Boolean
}

// Example
@Serializable data class GreetingResponse(val message: String)

fun Application.module(users: UserService) {
  routing {
    post("/greet/{name}") {
      val name = call.parameters["name"]
      if (name !in users) call.respond(HttpStatusCode.NotFound)
      else call.respond(HttpStatusCode.Created, GreetingResponse("Hello, $name"))
    }
  }
}
```

<DrawnAnnotation text="application/json" label="The format the client asked for"  :geometry="{ label: { x: 0.5535, y: 0.8178 } }"/>

```http
HTTP/1.1 201 Created
Content-Type: application/json

{ "message": "Hello, Alex" }
```

<!--
`respond` takes any object; the plug-in installed in lesson 1 decides how it
is written. Without `ContentNegotiation` this is a `500`: nothing knows how to
turn a `GreetingResponse` into bytes.
-->

---

# `Accept` chooses the response format

<DrawnAnnotation text="Content-Type: application/json" label="The format of this body" :geometry="{ label: { x: 0.5179, y: 0.2841, width: 0.3000 } }" />
<DrawnAnnotation text="Accept: application/json, application/xml" label="Formats the client can read, most preferred first" color="var(--fundamentals-pink)" :geometry="{ label: { x: 0.6353, y: 0.3719, width: 0.4000 } }" />

```http
PUT /user HTTP/1.1
Host: example.com
Content-Type: application/json
Accept: application/json, application/xml

{ "name": "Alex", "timezone": "CET" }
```

<DrawnAnnotation text="Content-Type: application/json" label="The server picked the first format it can produce" color="var(--fundamentals-pink)" :geometry="{ label: { x: 0.5147, y: 0.6151, width: 0.4500 } }" />

```http
HTTP/1.1 200 OK
Content-Type: application/json

{ "id": 12345 }
```

<!--
The body has no predefined format; server and client have to agree on one.
Formats are named by MIME type. `Content-Type` states the format of the body
that carries the header, `Accept` lists the formats the sender can read for
the response.
-->

---
magic-move
---

# `Accept` chooses the response format

<DrawnAnnotation text="application/xml, application/json" label="Same request body, a different preference" :geometry="{ label: { x: 0.7225, y: 0.3527, width: 0.4000 } }" />

```http
PUT /user HTTP/1.1
Host: example.com
Content-Type: application/json
Accept: application/xml, application/json

{ "name": "Alex", "timezone": "CET" }
```

<DrawnAnnotation text="application/xml" label="Same handler, same object, a different body" :geometry="{ label: { x: 0.5972, y: 0.5808, width: 0.4500 } }" />

```http
HTTP/1.1 200 OK
Content-Type: application/xml

<User id="12345"/>
```

<!--
Nothing changed in the handler between the two slides. Dealing with each
format by hand, and with the logic behind `Accept`, is exactly what the
`ContentNegotiation` plug-in takes off our hands.
-->

---

# One plug-in, one line per format

> No `Accept` header: the first registration wins

<DrawnAnnotation text="json()" label="`ktor-serialization-kotlinx-json`" :geometry="{ label: { x: 0.3435, y: 0.4095, width: 0.3600 } }" />
<DrawnAnnotation text="xml()" label="`ktor-serialization-kotlinx-xml`, one artifact per format" :geometry="{ label: { x: 0.4419, y: 0.4748, width: 0.7500 } }" />

```kotlin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.xml.xml
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.module() {
  install(ContentNegotiation) {
    json()
    xml()
  }
  routes()
}
```

<!--
`ContentNegotiation` itself is `ktor-server-content-negotiation`. Each
`json()`, `xml()`, `cbor()`, `protobuf()` registers a converter for one
MIME type; `receive` matches `Content-Type` against them, `respond` matches
`Accept`. Jackson and Gson converters exist for JSON too, but the deck uses
kotlinx.serialization: one annotation, every format.
-->

---

# Serialization is a compiler plug-in

<DrawnAnnotation text="kotlin(&quot;plugin.serialization&quot;)" label="Same version as Kotlin: serializers are generated at compile time" :geometry="{ label: { x: 0.7721, y: 0.3021, width: 0.4200 } }" />
<DrawnAnnotation text="kotlinx-serialization-json" label="One dependency per format: JSON, CBOR, ProtoBuf ship with the library"  :geometry="{ label: { x: 0.5812, y: 0.7144 } }"/>

```kotlin gradle no-compile
plugins {
  kotlin("jvm") version "2.4.20"
  kotlin("plugin.serialization") version "2.4.20"
}

dependencies {
  implementation("io.ktor:ktor-server-content-negotiation:3.6.0")
  implementation("io.ktor:ktor-serialization-kotlinx-json:3.6.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}
```

<!--
kotlinx.serialization comes from the Kotlin team and ships with the compiler.
Other libraries exist, from the JVM ecosystem or Kotlin-specific, more manual
or more automatic; this one is multiplatform and reflection-free. XML, YAML,
Avro and TOML formats are maintained by the community.
-->

---

# `@Serializable` derives the code

<DrawnAnnotation text="@Serializable" label="Every property with a backing field goes on the wire"  :geometry="{ label: { x: 0.5411, y: 0.2017 } }"/>
<DrawnAnnotation text="enum class Type" label="Nested classes need the annotation too; enums do not"  :geometry="{ label: { x: 0.3758, y: 0.7014 } }"/>

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class Greeting(
  val type: Type,
  val name: String,
  val timezone: String,
)

enum class Type { HELLO, BYE }
```

```json
{ "type": "HELLO", "name": "Alex", "timezone": "CET" }
```

<!--
The plug-in writes a `Greeting.serializer()` next to the class, with a
descriptor of the fields and the code to read and write them, format
agnostic. Every class used inside must be `@Serializable` as well, except
enums and the built-in types.
-->

---
magic-move
---

# `@Serializable` derives the code

<DrawnAnnotation text="@SerialName(&quot;tz&quot;)" label="The wire name; the Kotlin name stays" :geometry="{ label: { x: 0.3768, y: 0.4528, width: 0.4947 } }" />
<DrawnAnnotation text="@SerialName(&quot;hello&quot;)" label="Entries too, once the enum is `@Serializable`"  :geometry="{ label: { x: 0.4743, y: 0.5668 } }"/>

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Greeting(
  val type: Type,
  val name: String,
  @SerialName("tz") val timezone: String,
)

@Serializable
enum class Type {
  @SerialName("hello") HELLO,
  @SerialName("bye") BYE,
}
```

```json
{ "type": "hello", "name": "Alex", "tz": "CET" }
```

---
magic-move
---

# `@Serializable` derives the code

<DrawnAnnotation text="= Type.HELLO" label="Missing on the wire: the default fills in" :geometry="{ label: { x: 0.5753, y: 0.2944, width: 0.4161 } }" />
<DrawnAnnotation text="String? = null" label="Optional: absent and `null` both read back as `null`" color="var(--fundamentals-pink)" :geometry="{ label: { x: 0.6234, y: 0.4705, width: 0.3400 } }" />

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Greeting(
  val type: Type = Type.HELLO,
  val name: String,
  @SerialName("tz") val timezone: String? = null,
)

@Serializable
enum class Type {
  @SerialName("hello") HELLO,
  @SerialName("bye") BYE,
}
```

<DrawnAnnotation text="{ &quot;name&quot;: &quot;Alex&quot; }" label="Defaults are not serialized unless `encodeDefaults = true`" :geometry="{ label: { x: 0.3780, y: 0.8814 } }"/>

```json
{ "name": "Alex" }
```


---
magic-move
---

# DTOs are not the domain model

<DrawnAnnotation text="data class Greeting" label="The contract: rename a property here and every client notices" :geometry="{ label: { x: 0.66, y: 0.24, width: 0.40 } }" />
<DrawnAnnotation text="class Recipient" label="The domain type: no annotation, the real `TimeZone`" color="var(--fundamentals-blue)" :geometry="{ label: { x: 0.65, y: 0.475, width: 0.46 } }" />
<DrawnAnnotation text="TimeZone::of" label="The id travels as a `String`; resolve it where it is used" :geometry="{ label: { x: 0.64, y: 0.75, width: 0.40 } }" />

```kotlin
import kotlinx.datetime.TimeZone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Type {
  @SerialName("hello") HELLO,
  @SerialName("bye") BYE,
}

// Example
@Serializable
data class Greeting(
  val type: Type = Type.HELLO,
  val name: String,
  @SerialName("tz") val timezone: String? = null,
)

class Recipient(val name: String, val timezone: TimeZone)

fun Greeting.recipient(): Recipient =
  Recipient(name, timezone?.let(TimeZone::of) ?: TimeZone.UTC)
```

<!--
Keep the DTOs lean and separate from the domain model, so that a change to
one of them is visibly a change to the API; they can be shared between server
and client. The domain never sees `@SerialName` or a `String` that is really
a zone. kotlinx.datetime deprecates its `TimeZoneSerializer` on purpose:
resolving an id can fail on another machine with an older zone database, so
the wire carries the id and `TimeZone.of` resolves it here, where a failure
is this service's problem. An unknown id throws `IllegalTimeZoneException`
in the handler; lesson 7 turns that into a `400` with status pages.
-->

---

# `with =` replaces the generated serializer

<DrawnAnnotation text="with = NameSerializer::class" label="Private constructor, validating factory: nothing to derive" :geometry="{ label: { x: 0.745, y: 0.21, width: 0.26 } }" />
<DrawnAnnotation text="val name: Name" label="Validated on the way in, a `String` on the wire" :geometry="{ label: { x: 0.60, y: 0.665, width: 0.42 } }" />

```kotlin
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
enum class Type {
  @SerialName("hello") HELLO,
  @SerialName("bye") BYE,
}

object NameSerializer : KSerializer<Name> {
  override val descriptor =
    PrimitiveSerialDescriptor("Name", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Name) =
    encoder.encodeString(value.value)
  override fun deserialize(decoder: Decoder): Name =
    Name.of(decoder.decodeString())
      ?: throw SerializationException("name is blank")
}

// Example
@Serializable(with = NameSerializer::class)
class Name private constructor(val value: String) {
  companion object {
    fun of(raw: String): Name? = raw.trim().ifBlank { null }?.let(::Name)
  }
}

@Serializable
data class Greeting(
  val type: Type = Type.HELLO,
  val name: Name,
  @SerialName("tz") val timezone: String? = null,
)
```

<DrawnAnnotation text="&quot;Alex&quot;" label="Same wire shape as a `String`" :geometry="{ label: { x: 0.52, y: 0.845, width: 0.30 } }" />

```json
{ "name": "Alex" }
```

<!--
The plug-in derives a serializer from the primary constructor and the
properties with a backing field. It cannot know about `of`: a generated
serializer would call the private constructor directly and skip the
validation. `with =` points the class, or a single property, at a serializer
you write; the same mechanism covers a type from a library you do not own or
a legacy shape read through a surrogate class.
-->

---
magic-move
---

# A `KSerializer` is a descriptor and two functions

<DrawnAnnotation text="KSerializer<Name>" label="The interface the plug-in implements for you" :geometry="{ label: { x: 0.77, y: 0.47, width: 0.20 } }" />
<DrawnAnnotation text="PrimitiveSerialDescriptor(&quot;Name&quot;, PrimitiveKind.STRING)" label="Names the wire shape: still a string" :geometry="{ label: { x: 0.785, y: 0.625, width: 0.15 } }" />
<DrawnAnnotation text="throw SerializationException" label="A blank name is a `400` before the handler runs" color="red" :geometry="{ label: { x: 0.765, y: 0.86, width: 0.20 } }" />

```kotlin
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = NameSerializer::class)
class Name private constructor(val value: String) {
  companion object {
    fun of(raw: String): Name? = raw.trim().ifBlank { null }?.let(::Name)
  }
}

object NameSerializer : KSerializer<Name> {
  override val descriptor =
    PrimitiveSerialDescriptor("Name", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Name) =
    encoder.encodeString(value.value)
  override fun deserialize(decoder: Decoder): Name =
    Name.of(decoder.decodeString())
      ?: throw SerializationException("name is blank")
}
```

<!--
A descriptor that names the wire shape, and one function per direction:
`Encoder` and `Decoder` are format agnostic, so this serializer works for
JSON, CBOR and ProtoBuf alike. `ContentNegotiation` wraps whatever
`deserialize` throws in a `BadRequestException`, so the client sees a `400`
and the handler never sees a blank `Name`. For a class-shaped wire format,
delegate to a `@Serializable` surrogate class instead of encoding by hand;
the kotlinx.serialization guide has the recipe.
-->

---

# Dates and times are ISO 8601 strings

<DrawnAnnotation text="LocalDateTime" label="ISO 8601, from `kotlinx.datetime`" :geometry="{ label: { x: 0.71, y: 0.30, width: 0.34 } }" />
<DrawnAnnotation text="val timezone: String" label="No `TimeZone` serializer: send the id" color="var(--fundamentals-pink)" :geometry="{ label: { x: 0.71, y: 0.383, width: 0.34 } }" />
<DrawnAnnotation text="Instant" label="`kotlin.time.Instant` is built in" :geometry="{ label: { x: 0.71, y: 0.465, width: 0.34 } }" />

```kotlin
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Talk(
  val title: String,
  val startsAt: LocalDateTime,
  @SerialName("tz") val timezone: String,
  val createdAt: Instant,
)
```

```json
{
  "title": "Ktor Fundamentals",
  "startsAt": "2026-09-21T09:00",
  "tz": "Europe/Brussels",
  "createdAt": "2026-09-20T15:04:05Z"
}
```

<!--
`LocalDate`, `LocalTime`, `LocalDateTime` and `UtcOffset` from kotlinx.datetime
and `kotlin.time.Instant` from the standard library (kotlinx.serialization 1.9
and later) all serialize as the ISO 8601 string their `toString` produces, and
parse it back; component serializers exist for a JSON object instead.
`TimeZone` is the exception: kotlinx.datetime deprecated its serializer,
because an id such as `Europe/Brussels` only means something on a machine
whose zone database knows it. A local date and time plus a zone id is what a
calendar sends; an `Instant` is what a log or an audit trail sends.
-->

---

# The handler sees objects, not bytes

<DrawnAnnotation text="call.receive<Greeting>()" label="`Content-Type` picks the converter, `Greeting.serializer()` does the rest" :geometry="{ label: { x: 0.76, y: 0.384, width: 0.42 } }" />

```kotlin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Greeting(
  val type: Type = Type.HELLO,
  val name: String,
  @SerialName("tz") val timezone: String? = null,
)

@Serializable
enum class Type {
  @SerialName("hello") HELLO,
  @SerialName("bye") BYE,
}

// Example
fun Application.module() {
  install(ContentNegotiation) { json() }
  routing {
    post("/greet") {
      val greeting = call.receive<Greeting>()
      when (greeting.type) {
        Type.HELLO -> call.respondText("Hello, ${greeting.name}")
        Type.BYE -> call.respondText("Bye, ${greeting.name}")
      }
    }
  }
}
```

<!--
`Greeting` is the data class from the previous slides. A body that does not
parse, or a `Content-Type` nobody registered, ends in a `400` or `415` before
the handler runs. `receive` reads the body once: a second call throws
`RequestAlreadyConsumedException`, unless the `DoubleReceive` plug-in is
installed to cache it. A body that may be absent is `receive<Greeting?>()`,
since Ktor 3.6.
-->

---
magic-move
---

# The handler sees objects, not bytes

<DrawnAnnotation text="data class GreetingResponse" label="Typed on the way out too" :geometry="{ label: { x: 0.7191, y: 0.2431, width: 0.3000 } }" />
<DrawnAnnotation text="call.respond(GreetingResponse(message))" label="`Accept` picks the converter; `respondText` bypasses negotiation"  :geometry="{ label: { x: 0.4408, y: 0.8364 } }"/>

```kotlin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Greeting(
  val type: Type = Type.HELLO,
  val name: String,
  @SerialName("tz") val timezone: String? = null,
)

@Serializable
enum class Type {
  @SerialName("hello") HELLO,
  @SerialName("bye") BYE,
}

// Example
@Serializable
data class GreetingResponse(val message: String)

fun Application.module() {
  install(ContentNegotiation) { json() }
  routing {
    post("/greet") {
      val greeting = call.receive<Greeting>()
      val message = when (greeting.type) {
        Type.HELLO -> "Hello, ${greeting.name}"
        Type.BYE -> "Bye, ${greeting.name}"
      }
      call.respond(GreetingResponse(message))
    }
  }
}
```

<!--
Same shape on both sides: annotate, then `receive` and `respond` with your
own classes. What is next: the route and its parameters are still strings;
lesson 3 turns them into typed properties with delegation, and renders HTML
with `kotlinx.html` for the pages that are not JSON.
-->

---

# Ktor hides the wire, not the message

- `call.request.headers[…]`, `call.response.header(…)` → headers
- `{name}` → `call.parameters`, `?key=value` → `queryParameters`
- `call.receive<T>()`, `call.respond(status, T)` → the body, typed
- `receiveParameters()`, `receiveMultipart()` → forms and files
- `ContentNegotiation` + `@Serializable` → the conversion

> **The handler reads and writes Kotlin objects.**
>
> `Content-Type` and `Accept` decide the bytes.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Receive a greeting, respond with JSON

- Add a `POST /greet` route that receives a `@Serializable` DTO
- Respond with a `@Serializable` object; check the JSON in IntelliJ IDEA's HTTP client
- Make `timezone` optional with a default of `null`
- Return `400 Bad Request` when the name is blank
- Send `Accept: application/xml` and see what happens without `xml()`
