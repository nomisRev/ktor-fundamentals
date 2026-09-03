---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 4</div>

# Sessions and static content

## State across requests, files beside routes

---

# HTTP forgets, a session remembers

> Every request starts anew. A cart, a tracking id, a logged-in user have to survive it.

| Client-side | the data travels in a cookie or a header, and comes back on every request |
|-------------|---------------------------------------------------------------------------|
| Server-side | the data stays in memory or on disk; only an identifier travels          |

<!--
Sessions are the abstraction every web framework has for this; Ktor's is
typed. Cookies are handled by the browser for free; headers need the client
to cooperate, which suits programs better than people. JSON Web Tokens, the
machine-to-machine variant, are lesson 8.
-->

---

# A session is a registered class

<DrawnAnnotation text="cookie<GreetingTracker>(&quot;track&quot;)" label="A cookie named `track`, carrying the class: the data itself goes to the client" :geometry="{ label: { x: 0.72, y: 0.43, width: 0.44 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable

@Serializable
data class GreetingTracker(val greetings: Set<String>)

fun Application.module() {
  install(Sessions) {
    cookie<GreetingTracker>("track")
  }
  routes()
}
```

<!--
`ktor-server-sessions`. Three choices per registration: cookie or header,
its name, and where the data lives. With no storage argument the serialized
class is the cookie value.
-->

---
magic-move
---

# A session is a registered class

<DrawnAnnotation text="header<SecretInfo>" label="A header: for clients that are not browsers" :geometry="{ label: { x: 0.52, y: 0.72, width: 0.36 } }" />
<DrawnAnnotation text="SessionStorageMemory()" label="Server-side: the client only receives an identifier" :geometry="{ label: { x: 0.62, y: 0.83, width: 0.44 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.header
import kotlinx.serialization.Serializable

@Serializable
data class GreetingTracker(val greetings: Set<String>)

@Serializable
data class SecretInfo(val secret: String)

fun Application.module() {
  install(Sessions) {
    cookie<GreetingTracker>("track")
    header<SecretInfo>("secret", SessionStorageMemory())
  }
  routes()
}
```

<!--
Memory storage is per process: a restart or a second instance loses it.
`directorySessionStorage(File("build/.sessions"))` writes to disk; a shared
store such as Redis is a small interface away, `SessionStorage` has three
functions.
-->

---

# The handler reads and writes the session

<DrawnAnnotation text="call.sessions.get<GreetingTracker>()" label="`null` on the first visit: there is no cookie yet" :geometry="{ label: { x: 0.8, y: 0.34, width: 0.32 } }" />
<DrawnAnnotation text="call.sessions.set(" label="Serialized into the cookie of this response" :geometry="{ label: { x: 0.76, y: 0.43, width: 0.36 } }" />

```kotlin
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.Serializable

@Serializable
data class GreetingTracker(val greetings: Set<String>)

// Example
suspend fun RoutingContext.hello(req: Greeting.Hello) {
  val previous = call.sessions.get<GreetingTracker>()?.greetings.orEmpty()
  call.sessions.set(GreetingTracker(previous + "hello"))
  if ("bye" in previous) {
    call.respondText("I thought you were gone, ${req.parent.name}")
  } else {
    call.respondText("Hello, ${req.parent.name}")
  }
}
```

<!--
`get` and `set` are typed by the registered class. Nothing here says cookie
or header: the handler is the same whichever transport and storage were
chosen in `install`.
-->

---
magic-move
---

# The handler reads and writes the session

<DrawnAnnotation text="clear<GreetingTracker>()" label="Forget: the cookie is expired on this response" :geometry="{ label: { x: 0.74, y: 0.71, width: 0.36 } }" />

```kotlin
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.Serializable

@Serializable
data class GreetingTracker(val greetings: Set<String>)

// Example
suspend fun RoutingContext.hello(req: Greeting.Hello) {
  val previous = call.sessions.get<GreetingTracker>()?.greetings.orEmpty()
  call.sessions.set(GreetingTracker(previous + "hello"))
  if ("bye" in previous) {
    call.respondText("I thought you were gone, ${req.parent.name}")
  } else {
    call.respondText("Hello, ${req.parent.name}")
  }
}

suspend fun RoutingContext.bye(req: Greeting.Bye) {
  call.sessions.clear<GreetingTracker>()
  call.respondText("Bye, ${req.parent.name}")
}
```

---

# Session data is plain text by default

> Encrypted, only the server can read it. The client can still copy and replay it.

<DrawnAnnotation text="cookie<GreetingTracker>(&quot;track&quot;)" label="Readable and editable by whoever holds the cookie" color="red" :geometry="{ label: { x: 0.74, y: 0.55, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable

@Serializable
data class GreetingTracker(val greetings: Set<String>)

fun Application.module() {
  install(Sessions) {
    cookie<GreetingTracker>("track")
  }
  routes()
}
```

<!--
A session that says `admin = true` in the clear is an invitation. Sign it
so it cannot be edited, encrypt it so it cannot be read. Neither stops a
stolen cookie from being reused: that is what expiry and server-side
storage are for.
-->

---
magic-move
---

# Session data is plain text by default

<DrawnAnnotation text="SessionTransportTransformerEncrypt" label="AES plus an HMAC: encrypted and signed" :geometry="{ label: { x: 0.6, y: 0.77, width: 0.36 } }" />
<DrawnAnnotation text="hex(" label="Keys come from configuration, never from source: lesson 9" :geometry="{ label: { x: 0.78, y: 0.29, width: 0.3 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerEncrypt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.util.hex
import kotlinx.serialization.Serializable

@Serializable
data class GreetingTracker(val greetings: Set<String>)

val encryptKey = hex("00112233445566778899aabbccddeeff")
val signKey = hex("6819b57a326945c1968f45236589")

fun Application.module() {
  install(Sessions) {
    cookie<GreetingTracker>("track") {
      cookie.path = "/"
      transform(SessionTransportTransformerEncrypt(encryptKey, signKey))
    }
  }
  routes()
}
```

<!--
`SessionTransportTransformerMessageAuthentication(signKey)` signs without
encrypting, enough when the content is not secret. `cookie.path`,
`maxAgeInSeconds`, `secure`, `httpOnly` are set in the same block.
-->

---

# A server serves more than handlers

> Images, stylesheets, scripts: files, not routes

| A separate server | a proxy or a CDN in front decides who answers |
|-------------------|-----------------------------------------------|
| The same server   | one route hands out the files                |

<!--
Big deployments put static files behind a CDN. For everything else, and
for development, Ktor's routing does it: a static route is a route like
any other, so it composes with authentication, compression, and the rest
of the pipeline.
-->

---

# A folder is a route

<DrawnAnnotation text="&quot;/assets&quot;" label="The URL prefix" :geometry="{ label: { x: 0.62, y: 0.29, width: 0.24 } }" />
<DrawnAnnotation text="File(&quot;files&quot;)" label="The folder, relative to the working directory: `files/logo.png` is `/assets/logo.png`" :geometry="{ label: { x: 0.74, y: 0.39, width: 0.42 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.routing
import java.io.File

fun Application.module() {
  routing {
    staticFiles("/assets", File("files"))
  }
}
```

<!--
The folder name is not part of the URL. Subfolders are: `files/css/site.css`
is served at `/assets/css/site.css`, no nested declaration needed.
-->

---
magic-move
---

# A folder is a route

<DrawnAnnotation text="default(&quot;index.html&quot;)" label="Served for `/assets/` itself" :geometry="{ label: { x: 0.66, y: 0.34, width: 0.26 } }" />
<DrawnAnnotation text="preCompressed(CompressedFileType.GZIP)" label="`logo.png.gz` beside `logo.png`, served when the client accepts it" :geometry="{ label: { x: 0.83, y: 0.43, width: 0.3 } }" />
<DrawnAnnotation text="exclude" label="Never the dotfiles" :geometry="{ label: { x: 0.7, y: 0.56, width: 0.3 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.http.content.CompressedFileType
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.routing
import java.io.File

fun Application.module() {
  routing {
    staticFiles("/assets", File("files")) {
      default("index.html")
      preCompressed(CompressedFileType.GZIP)
      exclude { file -> file.name.startsWith(".") }
    }
  }
}
```

<!--
The block also has `contentType { }`, `cacheControl { }`, `etag`,
`lastModified`, and `fallback { }` for what to do when a file is missing.
-->

---
magic-move
---

# A folder is a route

<DrawnAnnotation text="staticResources(&quot;/assets&quot;, &quot;static&quot;)" label="From the classpath: `src/main/resources/static`, inside the jar" :geometry="{ label: { x: 0.74, y: 0.39, width: 0.4 } }" />
<DrawnAnnotation text="staticFiles(&quot;/uploads&quot;, File(&quot;uploads&quot;))" label="From disk: what changes while the server runs" :geometry="{ label: { x: 0.76, y: 0.29, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import java.io.File

fun Application.module() {
  routing {
    staticFiles("/uploads", File("uploads"))
    staticResources("/assets", "static")
  }
}
```

<!--
Same options for both. Resources ship with the application and are
read-only; files on disk can be written by the application or by an
operator. `staticZip` serves the content of an archive the same way.
-->

---

# A single page app is one file and many assets

> Every unknown path falls back to `index.html`: the router lives in the browser

<DrawnAnnotation text="useResources = true" label="From the classpath instead of the file system" :geometry="{ label: { x: 0.68, y: 0.5, width: 0.36 } }" />
<DrawnAnnotation text="react(&quot;app&quot;)" label="`app/index.html` and everything under it; `angular`, `vue`, `ember` too" :geometry="{ label: { x: 0.68, y: 0.6, width: 0.42 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.http.content.react
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    singlePageApplication {
      useResources = true
      react("app")
    }
  }
}
```

<!--
`react("app")` is `filesPath = "app"` plus the framework's default page.
`applicationRoute` mounts it somewhere other than `/`; `ignoreFiles { }`
keeps build artefacts out.
-->

---

# Compression is one line

> The defaults are fine: gzip and deflate, negotiated, only where it pays off

<DrawnAnnotation text="install(Compression)" label="Reads `Accept-Encoding`, writes `Content-Encoding`, skips images and small bodies" :geometry="{ label: { x: 0.7, y: 0.41, width: 0.44 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression

fun Application.module() {
  install(Compression)
  routes()
}
```

<!--
`ktor-server-compression`. The block accepts `gzip { priority = 1.0 }`,
`minimumSize(1024)`, and conditions per content type; static files with a
`preCompressed` variant skip the plug-in entirely.
-->

---

# The browser enforces the same-origin policy

> Front-end at `https://example.com`, API at `https://api.example.com`: two origins

<DrawnAnnotation text="Origin: https://example.com" label="Added by the browser to every cross-origin request" :geometry="{ label: { x: 0.72, y: 0.46, width: 0.4 } }" />

```http
GET /greet/Alex HTTP/1.1
Host: api.example.com
Origin: https://example.com
```

<DrawnAnnotation text="Access-Control-Allow-Origin" label="Without it the browser hides the response from the page" :geometry="{ label: { x: 0.76, y: 0.63, width: 0.4 } }" />

```http
HTTP/1.1 200 OK
Access-Control-Allow-Origin: https://example.com
```

<!--
The server answers either way; it is the browser that refuses to hand the
response to the script. Requests with credentials, non-simple content
types or custom headers get a preflight `OPTIONS` first, with the same
headers in play.
-->

---

# CORS is opt-in, by the server

<DrawnAnnotation text="anyHost()" label="Every origin: fine for a demo, not for production" color="red" :geometry="{ label: { x: 0.64, y: 0.29, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.module() {
  install(CORS) {
    anyHost()
  }
  routes()
}
```

<!--
`ktor-server-cors`. The plug-in answers the preflight and adds the
`Access-Control-*` headers to the real response; without it a
cross-origin call from a browser fails even though `curl` works.
-->

---
magic-move
---

# CORS is opt-in, by the server

<DrawnAnnotation text="allowHost(" label="`https://example.com`, `www.` and `app.`; `*.example.com` also works" :geometry="{ label: { x: 0.72, y: 0.29, width: 0.42 } }" />
<DrawnAnnotation text="schemes = listOf(&quot;https&quot;)" label="Say so explicitly: the default is `http` only" :geometry="{ label: { x: 0.74, y: 0.39, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.module() {
  install(CORS) {
    allowHost(
      "example.com",
      schemes = listOf("https"),
      subDomains = listOf("www", "app"),
    )
  }
  routes()
}
```

---
magic-move
---

# CORS is opt-in, by the server

<DrawnAnnotation text="allowCredentials = true" label="Cookies and `Authorization` may travel" :geometry="{ label: { x: 0.74, y: 0.53, width: 0.36 } }" />
<DrawnAnnotation text="allowNonSimpleContentTypes = true" label="`application/json` bodies are not simple" :geometry="{ label: { x: 0.76, y: 0.62, width: 0.36 } }" />
<DrawnAnnotation text="allowHeadersPrefixed(&quot;X-Greet-&quot;)" label="Custom headers, by prefix" :geometry="{ label: { x: 0.74, y: 0.71, width: 0.3 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.module() {
  install(CORS) {
    allowHost(
      "example.com",
      schemes = listOf("https"),
      subDomains = listOf("www", "app"),
    )
    allowCredentials = true
    allowNonSimpleContentTypes = true
    allowHeadersPrefixed("X-Greet-")
  }
  routes()
}
```

<!--
The allowed requests are deliberately narrow by default: `GET`, `POST`,
`HEAD`, a few headers, form content types. Everything a JSON API needs is
one line each, and each line is a conscious decision.
-->

---

# The session is a typed cookie, the folder is a route

- `install(Sessions) { cookie<T>("name") }` → state per client, typed
- `call.sessions.get<T>()`, `set(…)`, `clear<T>()` → read, write, forget
- `SessionTransportTransformerEncrypt` → never plain text
- `staticFiles`, `staticResources`, `singlePageApplication` → files as routes
- `Compression`, `CORS` → the wire's size, the browser's rules

> **Both are plug-ins on the same pipeline.**
>
> Nothing in the handler knows about either.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Remember the visitor, serve the assets

- Count visits in a `@Serializable` session class stored in a signed cookie
- Show the count on `/greet/{name}/hello`, clear it on `/greet/{name}/bye`
- Serve `src/main/resources/static` at `/assets`: a logo and a stylesheet the HTML page links
- Allow only your front-end's origin with `CORS`, then call the API from another one
