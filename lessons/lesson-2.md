---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 2</div>

# Server and configuration

## Own your `main`, fail fast on configuration

---

# Every server has something to configure

| The listener          | host and port, TLS: where the server accepts connections   |
|-----------------------|------------------------------------------------------------|
| Plug-ins and services | database URLs, API endpoints, cache sizes, time-outs       |
| Secrets               | API keys, signing keys, passwords: never the same twice    |

<!--
Even the one-line server of lesson 1 had a port and a host in it. As
soon as plug-ins arrive, so do their settings: where the database is,
which GitHub endpoint to call, how large the cache may grow. And the
last row is the one that matters most: the API token of lesson 6, the
JWT secret of lesson 9, anything that must differ between a laptop,
staging and production. This lesson is about where those values live
and how the application reads them, before a single route is written;
lesson 10 takes the result to a container.
-->

---

# `embeddedServer` configures in code

<DrawnAnnotation text="port = 8080, host = &quot;0.0.0.0&quot;" label="Lesson 1: two parameters, in the source" :geometry="{ label: { x: 0.72, y: 0.29, width: 0.34 } }" />

```kotlin
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") { module() }
    .start(wait = true)
}
```

<!--
The way lesson 1 ran a Ktor server, and the one every lesson will keep
using: `embeddedServer` takes the engine, the listener, and the module.
Owning `main` like this is deliberate. Nothing starts behind your back,
the order of things is in the source, and `testApplication` in lesson 8
has the same shape. Whatever needs configuring is a Kotlin value here,
which is exactly right for a test and wrong for production, where the
port is decided by whoever runs the container. Changing it means
recompiling.
-->

---
magic-move
---

# `embeddedServer` configures in code

<DrawnAnnotation text="configure = {" label="Engine options: thread pools, time-outs, and the connectors" :geometry="{ label: { x: 0.7, y: 0.336, width: 0.4 } }" />
<DrawnAnnotation text="connector {" label="One listener; add another for TLS on `8443`" :geometry="{ label: { x: 0.7, y: 0.47, width: 0.36 } }" />

<TypeHint :line="4" receiver="NettyApplicationEngine.Configuration">

```kotlin
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
  embeddedServer(
    Netty,
    configure = {
      connector {
        host = "0.0.0.0"
        port = 8080
      }
    },
  ) { module() }.start(wait = true)
}
```

</TypeHint>

<!--
The `port` and `host` parameters are a shortcut for this. `configure`
receives the engine's configuration class, so the options depend on the
engine: `NettyApplicationEngine.Configuration` has `requestQueueLimit`
and `shareWorkGroup`, CIO has `connectionIdleTimeoutSeconds`. The
`connector` block is common to all of them, and it repeats: one for plain
HTTP, one `sslConnector` with the key store for HTTPS.
-->

---

# The engine is a dependency

| `Netty`                    | `ktor-server-netty`: JVM, HTTP/2, the default of `start.ktor.io`    |
|----------------------------|---------------------------------------------------------------------|
| `Jetty`                    | `ktor-server-jetty-jakarta`: JVM, HTTP/2                            |
| `Tomcat`                   | `ktor-server-tomcat-jakarta`: JVM, HTTP/2                           |
| `CIO`                      | `ktor-server-cio`: coroutines only; JVM, Native, GraalVM, no HTTP/2 |
| `ServletApplicationEngine` | `ktor-server-servlet-jakarta`: inside a container, lesson 10        |

<!--
The engine is the thing that listens on the socket and turns bytes into
`ApplicationCall`s. The application does not know which one it runs on:
change the dependency and the factory object, nothing else. Netty is the
default and the most tuned; Jetty and Tomcat matter when an organisation
already runs them; CIO is written in Kotlin coroutines, so it is the one
that compiles to a native binary. The `-jakarta` suffix is the Servlet
5+ API: the old `javax` artifacts are gone from Ktor 3.
-->

---

# Configuration lives outside the code

<DrawnAnnotation text="$HOST" label="An environment variable, or a system property" :geometry="{ label: { x: 0.4929, y: 0.2, width: 0.34 } }" />
<DrawnAnnotation text="0.0.0.0" label="The fallback, after the colon" :geometry="{ label: { x: 0.8064, y: 0.2, width: 0.2472 } }" />

```yaml
host: "$HOST:0.0.0.0"
port: "$PORT:8080"
```

<!--
The values that differ per deployment go in a file next to the code, not
in it. YAML is the format the people who run the service already read,
and `ktor-server-config-yaml` reads it for us; HOCON works the same way
and gets a slide of its own later. Every string can reference an
environment variable or a system property with the same `$` syntax as a
Kotlin string template: `$HOST` is the variable, `${HOST}` works too, and
what follows the colon is the fallback when it is not set. A `$$` keeps
a literal dollar.
-->

---
magic-move
---

# Configuration lives outside the code

<DrawnAnnotation text="&quot;$HOST&quot;" label="No fallback: a mandatory value, the server fails fast at start-up" :geometry="{ label: { x: 0.5, y: 0.19, width: 0.36 } }" />

```yaml
host: "$HOST"
port: "$PORT:8080"
```

<!--
Leave the fallback out and the value is mandatory: a process started
without `HOST` refuses to start, with the missing key in the message.
That is the failure you want, loud and early, instead of a server that
answers on the wrong interface or signs tokens with an empty secret.
Configuration is the boundary that lets the same build run anywhere, so
it is the one place to be strict.
-->

---

# Configuration is a `@Serializable` data class

<DrawnAnnotation text="val host: String" :geometry="{ label: { x: 0.5, y: 0.3 } }" />
<DrawnAnnotation text="host:" label="The same key-value pairs as the YAML file" :geometry="{ label: { x: 0.4707, y: 0.292, width: 0.34 } }" />

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class Config(
  val host: String,
  val port: Int,
)
```

```yaml
host: "$HOST:0.0.0.0"
port: "$PORT:8080"
```

<!--
The file needs a type on the Kotlin side, and it is a plain data class
with one property per key. `@Serializable` is the kotlinx.serialization
compiler plug-in, the same one that will derive the JSON DTOs of lesson
3: the generated serializer knows the property names and types, and Ktor
uses it to decode the configuration, so `"8080"` in the file becomes an
`Int` here and a missing or misspelled key is an error at start-up.
-->

---
magic-move
---

# Configuration is a `@Serializable` data class

<DrawnAnnotation text="Environment" label="Primitives, collections and enums; keep complex types out of the file" :geometry="{ label: { x: 0.6038, y: 0.384, width: 0.34 } }" />

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class Config(
  val host: String,
  val port: Int,
  val environment: Environment,
)

@Serializable
enum class Environment { PROD, STAGING, DEV }
```

```yaml
host: "$HOST:0.0.0.0"
port: "$PORT:8080"
environment: "$ENV:DEV"
```

<!--
Enums decode by name, lists and nested classes work, and any
`@Serializable` type would, but resist fitting more into the file than
it should hold: primitives, collections, enumerations. A configuration
is read by operators, diffed between environments and templated by a
deployment tool, so the simpler its shape the better.
-->

---

# Configuration grows by feature

<DrawnAnnotation text="val github: GitHub" label="One top-level key per feature: a nested class for each" :geometry="{ label: { x: 0.54, y: 0.338, width: 0.34 } }" />
<DrawnAnnotation text="&quot;$GITHUB_TOKEN&quot;" label="No fallback: a secret has no default" :geometry="{ label: { x: 0.62, y: 0.62, width: 0.34 } }" />

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class Server(val host: String, val port: Int)

@Serializable
data class GitHub(val token: String)

// Example
@Serializable
data class Config(val server: Server, val github: GitHub)
```

```yaml
server:
  host: "$HOST:0.0.0.0"
  port: "$PORT:8080"

github:
  token: "$GITHUB_TOKEN"
```

<!--
A flat list of keys stops working at the third feature. Group the
settings of each feature under its own key, and mirror the tree in the
data class: `server` for the listener, `github` for the service lesson 6
calls, `security` for the JWT of lesson 9. The token has no fallback on
purpose; there is no sensible default for a secret, and a laptop without
`GITHUB_TOKEN` should find out at start-up.
-->

---
magic-move
---

# Configuration grows by feature

<DrawnAnnotation text="data class GitHub" label="Data classes can live anywhere: the feature owns its settings" :geometry="{ label: { x: 0.7, y: 0.46, width: 0.34 } }" />

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class Config(val server: Server, val github: GitHub)

@Serializable
data class Server(val host: String, val port: Int)

@Serializable
data class GitHub(val token: String)
```

```yaml
server:
  host: "$HOST:0.0.0.0"
  port: "$PORT:8080"

github:
  token: "$GITHUB_TOKEN"
```

<!--
Each section is its own class, and nothing says the classes must sit in
one file. The GitHub client of lesson 6 can declare `GitHub` next to
itself and take one as a constructor argument; the root `Config` only
assembles them. Modules of a larger build share configuration the same
way, by sharing the class.
-->

---

# Load it at runtime with `ApplicationConfig`

<DrawnAnnotation text="ApplicationConfig(&quot;application.yaml&quot;)" label="Reads the file from the classpath, substitutes the variables" :geometry="{ label: { x: 0.3, y: 0.43, width: 0.34 } }" />
<DrawnAnnotation text="getAs<Config>()" label="Deserialises with kotlinx.serialization" :geometry="{ label: { x: 0.7874, y: 0.246, width: 0.2853 } }" />

```kotlin
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import kotlinx.serialization.Serializable

@Serializable
data class Config(val host: String, val port: Int)

val config: Config = ApplicationConfig("application.yaml").getAs<Config>()
```

<!--
`ApplicationConfig` is Ktor's view of a configuration file, whichever
format it came in: the function loads the resource, picks the loader for
its extension, and substitutes the `$VARIABLE` references. `getAs` is the
kotlinx.serialization integration, added in Ktor 3.2: the file's tree
becomes the data class in one call, and the exception names the key that
was missing. No extra library, no reflection, the same serializer the
compiler plug-in already generated.
-->

---
magic-move
---

# Load it at runtime with `ApplicationConfig`

<DrawnAnnotation text="mergeWith" label="Later files win: compose one configuration from several" :geometry="{ label: { x: 0.8262, y: 0.568, width: 0.2076 } }" />

```kotlin
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.config.mergeWith
import kotlinx.serialization.Serializable

@Serializable
data class GitHub(val token: String)

// Example
@Serializable
data class Config(val host: String, val port: Int, val github: GitHub)

val config: Config = ApplicationConfig("application.yaml")
  .mergeWith(ApplicationConfig("github.yaml"))
  .getAs<Config>()
```

<!--
One file per concern, or a base file and an override per environment:
`mergeWith` combines two configurations key by key, and where both have
a key the argument wins. `ConfigLoader.loadAll("a.yaml", "b.yaml")` does
the same for a list. The result is still an `ApplicationConfig`, so
`getAs` at the end sees one tree.
-->

---

# Load the config first, then start the server

<DrawnAnnotation text="host = config.server.host" label="Fail fast: no server without a valid configuration" :geometry="{ label: { x: 0.7, y: 0.31, width: 0.34 } }" />
<DrawnAnnotation text="module(config)" label="The module gets the values it needs as an argument" :geometry="{ label: { x: 0.7, y: 0.5, width: 0.34 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.Serializable

@Serializable
data class Config(val server: Server, val github: GitHub)

@Serializable
data class Server(val host: String, val port: Int)

@Serializable
data class GitHub(val token: String)

fun Application.module(config: Config) {
  routes()
}

// Example
fun main() {
  val config = ApplicationConfig("application.yaml").getAs<Config>()
  embeddedServer(
    Netty,
    host = config.server.host,
    port = config.server.port,
  ) { module(config) }.start(wait = true)
}
```

<!--
Back to `main`, which is why owning it matters: the first line loads the
configuration, and only when that succeeded does the server start. A
missing `GITHUB_TOKEN` stops the process before a port is bound, so
nothing half-configured ever answers a request. The engine takes its
listener from the loaded values, and the module receives the rest as a
plain argument: no global, no service locator, and a test can pass a
`Config` of its own.
-->

---

# Secrets never live in the source

> Not in git, not in the image, not in a log line

<DrawnAnnotation text="&quot;ghp_sup3rs3cret&quot;" label="In every clone, every build, every image layer" color="red" :geometry="{ label: { x: 0.62, y: 0.305, width: 0.36 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth

val token = "ghp_sup3rs3cret"

val client = HttpClient(CIO) {
  defaultRequest { bearerAuth(token) }
}
```

<!--
The slides will hard-code the odd value for the sake of the example, and
this one is marked red for a reason. A secret in the source is in every
clone of the repository, every CI log that prints the build, and every
layer of the image: rotating it means a release. The client is lesson
6's; the point is the string above it. And be careful what the
application logs: a `toString` of a config class, or a debug line of a
plug-in, can print the secret into the very logs everyone can read.
-->

---
magic-move
---

# Secrets never live in the source

> Not in git, not in the image, not in a log line

<DrawnAnnotation text="config.github.token" />
<DrawnAnnotation text="&quot;$GITHUB_TOKEN&quot;" label="Set by the platform: a vault, a Kubernetes secret, the CI's variables" :geometry="{ label: { x: 0.7, y: 0.62, width: 0.42 } }" />

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import kotlinx.serialization.Serializable

@Serializable
data class Config(val github: GitHub)

@Serializable
data class GitHub(val token: String)

// Example
val config = ApplicationConfig("application.yaml").getAs<Config>()

val client = HttpClient(CIO) {
  defaultRequest { bearerAuth(config.github.token) }
}
```

```yaml
github:
  token: "$GITHUB_TOKEN"
```

<!--
The fix is the file from a few slides ago and a key without a fallback:
a container started without the variable refuses to start instead of
calling GitHub with an empty token. Cloud providers offer vaults and
inject their values as environment variables or mounted files; either
way the code only ever sees `config.github.token`.
-->

---

# `EngineMain` reads a file instead

> The other `main`: the file names the module

<DrawnAnnotation text="args" label="`-port=8081`, `-config=prod.yaml`: the command line overrides the file" :geometry="{ label: { x: 0.72, y: 0.305, width: 0.44 } }" />
<DrawnAnnotation text="io.ktor.server.netty.EngineMain" label="The engine is in the name: `cio`, `jetty.jakarta`, `tomcat.jakarta`" :geometry="{ label: { x: 0.76, y: 0.42, width: 0.38 } }" />

```kotlin
fun main(args: Array<String>): Unit =
  io.ktor.server.netty.EngineMain.main(args)
```

<!--
The second way to run a server, and the one `start.ktor.io` generates.
`main` no longer says anything about the application: the engine's
`EngineMain` object loads `application.yaml` or `application.conf` from
the classpath, reads the port and the list of modules from it, and
starts. Command-line arguments win over the file: `-port`, `-host`,
`-config` for another file, and `-P:ktor.deployment.callGroupSize=7` for
any key at all. Convenient, and you will meet it in every generated
project; the price is that the start-up order is now Ktor's, not yours.
-->

---

# The file says what `main` used to say

<DrawnAnnotation text="ktor:" label="Everything Ktor reads for itself, next to your own keys" :geometry="{ label: { x: 0.3, y: 0.19, width: 0.36 } }" />
<DrawnAnnotation text="port: &quot;$PORT:8080&quot;" label="`ktor.deployment`: what `connector { }` set in code" :geometry="{ label: { x: 0.66, y: 0.30, width: 0.38 } }" />
<DrawnAnnotation text="com.example.ApplicationKt.module" label="A top-level `module` in `Application.kt`: the class is `ApplicationKt`" :geometry="{ label: { x: 0.7, y: 0.5, width: 0.44 } }" />

```yaml
ktor:
  deployment:
    port: "$PORT:8080"
  application:
    modules:
      - com.example.ApplicationKt.module

github:
  token: "$GITHUB_TOKEN"
```

<!--
The same YAML file, with one reserved key. Everything Ktor reads for
itself is under `ktor`: `deployment` for the listener and the engine's
thread pools, `application.modules` for the functions to call on
start-up. The module reference is a JVM class name plus a function:
Kotlin compiles top-level functions of `Application.kt` into a class
called `ApplicationKt`, which is why the name looks the way it does.
Several modules can be listed, and they run in order. Outside the `ktor`
block the file is yours, exactly as before.
-->

---
magic-move
---

# Development mode is a key too

<DrawnAnnotation text="development: true" label="Or `-Dio.ktor.development=true`: the `500` page shows the stack trace, classes reload" :geometry="{ label: { x: 0.625, y: 0.242, width: 0.65 } }" />
<DrawnAnnotation text="watch:" label="Which paths to watch; with `./gradlew -t build` beside it, a change is live" :geometry="{ label: { x: 0.57, y: 0.41, width: 0.7 } }" />

```yaml
ktor:
  development: true
  deployment:
    port: "$PORT:8080"
    watch:
      - classes
  application:
    modules:
      - com.example.ApplicationKt.module

github:
  token: "$GITHUB_TOKEN"
```

<!--
Two things change in development mode. An exception in a handler
answers with the stack trace in the body instead of an empty `500`,
which is what you want on a laptop and never in production. And the
engine watches the paths under `watch` for changed class files and
reloads the application without restarting the JVM, so with Gradle's
continuous build running next to the server a saved file is a live
route a few seconds later. The IDE sets the system property in the run
configuration; `start.ktor.io` projects have it in the Gradle
`application` block. The key is read at start-up, so leave it out of
the file that ships and set the property instead.
-->

---

# `application.conf` says the same

<DrawnAnnotation text="ktor {" label="HOCON: JSON with the braces and quotes made optional" :geometry="{ label: { x: 0.64, y: 0.195, width: 0.42 } }" />
<DrawnAnnotation text="${?PORT}" label="Overrides `8080` when `PORT` exists; without `?` a missing one fails" :geometry="{ label: { x: 0.66, y: 0.336, width: 0.44 } }" />

```text
ktor {
  deployment {
    port = 8080
    port = ${?PORT}
  }
  application {
    modules = [ com.example.ApplicationKt.module ]
  }
}

github {
  token = ${GITHUB_TOKEN}
}
```

<!--
The same tree in HOCON, Human-Optimized Config Object Notation, from the
Typesafe Config library that `ktor-server-core` already brings in. The
keys are identical, so nothing in the code changes; only the
substitution syntax differs. HOCON keeps the last assignment of a key,
and `${?PORT}` is skipped when the variable is absent, which is how a
default is spelled; `${GITHUB_TOKEN}` without the question mark is
mandatory. Both formats are equally supported, pick the one your team
reads fluently.
-->

---

# The module reads its own keys

<DrawnAnnotation text="environment.config" label="`ApplicationConfig`: the file, the command line, or a map from `main`" :geometry="{ label: { x: 0.7, y: 0.33, width: 0.42 } }" />
<DrawnAnnotation text="getString()" label="Also `getList()`; `propertyOrNull` for an optional key" :geometry="{ label: { x: 0.72, y: 0.44, width: 0.44 } }" />

```kotlin
import io.ktor.server.application.Application

fun Application.module() {
  val token = environment.config.property("github.token").getString()
  routes()
}
```

<!--
With `EngineMain` nobody passes a `Config` to the module, so the module
asks. `Application.environment` is the `ApplicationEnvironment`: the
logger and the configuration, whichever way it was loaded. `property`
takes the dotted path and throws an `ApplicationConfigurationException`
when it is absent, so the server never starts without its token;
`propertyOrNull` is for settings with a sensible default in code. The
value converts on demand: `getString`, `getList`, `getMap`.
-->

---
magic-move
---

# The module reads its own keys

<DrawnAnnotation text="property(&quot;github&quot;)" label="A whole section, deserialised into the feature's class" :geometry="{ label: { x: 0.7, y: 0.44, width: 0.42 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.config.property
import kotlinx.serialization.Serializable

@Serializable
data class GitHub(val token: String)

fun Application.module() {
  val github: GitHub = property("github")
  routes()
}
```

<!--
The typed form of the same lookup. `Application.property<T>` reads one
section and runs it through `getAs`, so the feature's class from the
grows-by-feature slide works here unchanged, and `environment.config.getAs<Config>()`
decodes the whole file at once when the module wants the root; the
`ktor` block is simply not part of the class and is skipped. Either way
the module reads as a function of typed inputs, which is also how a test
calls it.
-->

---
magic-move
---

# The module reads its own keys

<DrawnAnnotation text="@Property(&quot;github&quot;)" label="Ktor 3.2+: `ktor-server-di` fills the parameter from the file, lesson 6" :geometry="{ label: { x: 0.62, y: 0.21, width: 0.42 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.annotations.Property
import kotlinx.serialization.Serializable

@Serializable
data class GitHub(val token: String)

fun Application.module(@Property("github") github: GitHub) {
  routes()
}
```

<!--
Or do not ask at all. When the module is listed in `application.yaml`
and the DI plug-in of lesson 6 is on the classpath, module parameters
are resolved like dependencies: `@Property` names the configuration key,
and the parameter's class decides the shape, a `String` for one value or
a data class for a section. The module no longer knows there is a file.
-->

---

# `embeddedServer` can carry a configuration

<DrawnAnnotation text="applicationEnvironment {" label="The environment: configuration and logger; the connectors stay in `configure`" :geometry="{ label: { x: 0.74, y: 0.30, width: 0.4 } }" />
<DrawnAnnotation text="MapApplicationConfig" label="In-memory keys: what a test hands the module" :geometry="{ label: { x: 0.76, y: 0.47, width: 0.34 } }" />

```kotlin
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
  embeddedServer(
    Netty,
    environment = applicationEnvironment {
      config = MapApplicationConfig("github.token" to "ghp_test")
    },
    configure = { connector { port = 8080 } },
  ) { module() }.start(wait = true)
}
```

<!--
The two worlds meet: a module written against `environment.config` does
not care whether `EngineMain` or `embeddedServer` filled it. The
`environment` parameter builds an `ApplicationEnvironment` by hand, and
`MapApplicationConfig` is the simplest `ApplicationConfig`: pairs of
dotted key and value. `testApplication` does exactly this with its
`environment { config = MapApplicationConfig(...) }` block, so a test can
run the module with its own settings.
-->

---
magic-move
---

# `embeddedServer` can carry a configuration

<DrawnAnnotation text="ApplicationConfig(&quot;application.yaml&quot;)" label="The file, without `EngineMain`: `main` reads it, the modules see it" :geometry="{ label: { x: 0.76, y: 0.47, width: 0.36 } }" />

```kotlin
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
  embeddedServer(
    Netty,
    environment = applicationEnvironment {
      config = ApplicationConfig("application.yaml")
    },
    configure = { connector { port = 8080 } },
  ) { module() }.start(wait = true)
}
```

<!--
The same loader as the `getAs` slide, handed to the environment instead
of decoded in `main`: modules that use `property` or `@Property` get the
file, and `main` keeps the start-up order. It is the same object
`EngineMain` would have built, minus the part where Ktor reads
`ktor.deployment.port` for you. For the command line,
`CommandLineConfig(args)` parses the flags `EngineMain` accepts:
`configure = { takeFrom(CommandLineConfig(args).engineConfig) }` brings
`-port=8080` to an embedded server.
-->

---

# Configuration comes from outside the code

- `embeddedServer(Netty, host = config.host, …)` → your `main`, your order
- `"$PORT:8080"` → the environment, with a fallback; none for a secret
- `@Serializable data class Config(...)` → one class per feature
- `ApplicationConfig(path).getAs<Config>()` → the file as a type
- `EngineMain`, `property("github")`, `@Property` → when the file starts the server

> **Load the configuration first, fail fast, then start.**
>
> The same build runs on every port, with every secret.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Configure the greeting service from a file

- Put the host and the port in `application.yaml`, `"$PORT:8080"` for the port
- Declare a `@Serializable` `Config` with a `Server` section
- Load it in `main` with `getAs<Config>()`
- Start `embeddedServer` from the loaded values
- Add a mandatory `github.token` and start without it
