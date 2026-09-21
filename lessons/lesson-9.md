---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 9</div>

# Configuration and deployment

## From `application.conf` to a container

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
last row is the one that matters most: the JWT secret of lesson 8, the
API keys, anything that must differ between a laptop, staging and
production. This lesson is about where those values live, how the
application reads them, and how the whole thing leaves the laptop.
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
The first way to run a Ktor server, and the one every lesson so far has
used: `embeddedServer` takes the engine, the listener, and the module.
Whatever needs configuring is a Kotlin value here, which is exactly right
for a test (`testApplication` is the same shape) and wrong for production,
where the port is decided by whoever runs the container. Changing it
means recompiling.
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
| `ServletApplicationEngine` | `ktor-server-servlet-jakarta`: inside a container, see the WAR      |

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

# `EngineMain` reads a file instead

> The same `main` everywhere: the file names the module

<DrawnAnnotation text="args" label="`-port=8081`, `-config=prod.conf`: the command line overrides the file" :geometry="{ label: { x: 0.72, y: 0.305, width: 0.44 } }" />
<DrawnAnnotation text="io.ktor.server.netty.EngineMain" label="The engine is in the name: `cio`, `jetty.jakarta`, `tomcat.jakarta`" :geometry="{ label: { x: 0.76, y: 0.42, width: 0.38 } }" />

```kotlin
fun main(args: Array<String>): Unit =
  io.ktor.server.netty.EngineMain.main(args)
```

<!--
The second way to run a server. `main` no longer says anything about the
application: the engine's `EngineMain` object loads `application.conf`
or `application.yaml` from the classpath, reads the port and the list of
modules from it, and starts. The generated projects of `start.ktor.io`
use this form. Command-line arguments win over the file: `-port`,
`-host`, `-config` for another file, and `-P:ktor.deployment.callGroupSize=7`
for any key at all.
-->

---

# The file says what `main` used to say

<DrawnAnnotation text="port = 8080" label="`ktor.deployment`: what `connector { }` set in code" :geometry="{ label: { x: 0.66, y: 0.289, width: 0.38 } }" />
<DrawnAnnotation text="com.example.ApplicationKt.module" label="A top-level `module` in `Application.kt`: the class is `ApplicationKt`" :geometry="{ label: { x: 0.7, y: 0.5, width: 0.44 } }" />

```text
ktor {
  deployment {
    port = 8080
  }
  application {
    modules = [ com.example.ApplicationKt.module ]
  }
}
```

<!--
HOCON, Human-Optimized Config Object Notation, is JSON with the braces
and quotes made optional, from the Typesafe Config library. Everything
Ktor reads for itself is under `ktor`: `deployment` for the listener and
the engine's thread pools, `application.modules` for the functions to
call on start-up. The module reference is a JVM class name plus a
function: Kotlin compiles top-level functions of `Application.kt` into a
class called `ApplicationKt`, which is why the name looks the way it
does. Several modules can be listed, and they run in order.
-->

---
magic-move
---

# The file says what `main` used to say

<DrawnAnnotation text="${?PORT}" label="Overrides `8080` when the `PORT` variable exists; without `?` a missing one fails" :geometry="{ label: { x: 0.7, y: 0.336, width: 0.44 } }" />

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
```

<!--
This is the idiom that makes one file serve every environment. HOCON
keeps the last assignment of a key, and `${?PORT}` is a substitution
that is skipped when the environment variable is absent, so the laptop
gets `8080` and the container gets whatever the platform hands it.
`${PORT}` without the question mark is required: the server refuses to
start when it is unset, which is often what you want for a secret.
-->

---
magic-move
---

# The file says what `main` used to say

<DrawnAnnotation text="${GITHUB_TOKEN}" label="Any key you like: the module reads it, next" :geometry="{ label: { x: 0.68, y: 0.665, width: 0.38 } }" />

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
Outside the `ktor` block the file is yours. Group the settings of each
service or plug-in under its own key, and let the environment fill in
the values that differ per deployment. Nothing here is validated by Ktor;
the module that reads `github.token` decides what happens when it is
missing.
-->

---

# `application.yaml` says the same

<DrawnAnnotation text="ktor:" label="Needs `ktor-server-config-yaml`; found by its name, like the `.conf`" :geometry="{ label: { x: 0.64, y: 0.195, width: 0.42 } }" />
<DrawnAnnotation text="&quot;$PORT:8080&quot;" label="`$PORT`, with the default after the colon" :geometry="{ label: { x: 0.66, y: 0.30, width: 0.36 } }" />
<DrawnAnnotation text="&quot;$GITHUB_TOKEN&quot;" label="Both `$ENV` and `${ENV}` work in YAML" :geometry="{ label: { x: 0.66, y: 0.571, width: 0.34 } }" />

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
The same tree in YAML. The keys are identical, so the module code does
not change; only the substitution syntax differs, and the default comes
after a colon instead of a second assignment. YAML needs one extra
artifact because Ktor's HOCON support rides on Typesafe Config, which
`ktor-server-core` already brings in. `start.ktor.io` generates the YAML
version today; both are equally supported, pick the one your team reads
fluently.
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

# The module reads its own keys

<DrawnAnnotation text="environment.config" label="`ApplicationConfig`: the file, the command line, or a map from `main`" :geometry="{ label: { x: 0.7, y: 0.33, width: 0.42 } }" />
<DrawnAnnotation text="getString()" label="Also `getList()`, and `getAs<T>()` for a data class" :geometry="{ label: { x: 0.72, y: 0.44, width: 0.44 } }" />

```kotlin
import io.ktor.server.application.Application

fun Application.module() {
  val token = environment.config.property("github.token").getString()
  routes()
}
```

<!--
`Application.environment` is the `ApplicationEnvironment`: the logger and
the configuration, whichever way it was loaded. `property` takes the
dotted path and returns an `ApplicationConfigValue`, which converts on
demand: `getString`, `getList`, and since 3.2 `getAs<T>()` with a
`@Serializable` class for a whole section. This is where lesson 8's
`secret` should have come from.
-->

---
magic-move
---

# The module reads its own keys

<DrawnAnnotation text="propertyOrNull" label="Optional: `null` when the key is absent; `property` throws at start-up" :geometry="{ label: { x: 0.7, y: 0.39, width: 0.42 } }" />

```kotlin
import io.ktor.server.application.Application

fun Application.module() {
  val token = environment.config.property("github.token").getString()
  val name = environment.config.propertyOrNull("app.name")?.getString()
  routes()
}
```

<!--
Two ways to ask, one decision: is the key required? `property` throws an
`ApplicationConfigurationException` while the module runs, so the server
never starts without its token, which is the failure you want, loud and
early. `propertyOrNull` is for settings with a sensible default in code.
-->

---
magic-move
---

# The module reads its own keys

<DrawnAnnotation text="@Property(&quot;github.token&quot;)" label="Ktor 3.2+: `ktor-server-di` fills the parameter from the file's key" :geometry="{ label: { x: 0.7, y: 0.30, width: 0.42 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.annotations.Property

fun Application.module(@Property("github.token") token: String) {
  routes()
}
```

<!--
Or do not ask at all. When the module is listed in `application.conf`
and the DI plug-in of lesson 5 is on the classpath, module parameters
are resolved like dependencies: `@Property` names the configuration key,
and a data class parameter maps a whole section. The module reads as a
function of its inputs, which is also how a test calls it.
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
      config = MapApplicationConfig("app.name" to "Awesome Ktor app")
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

<DrawnAnnotation text="HoconApplicationConfig(ConfigFactory.load())" label="Typesafe Config reads `application.conf` from the classpath: the file, without `EngineMain`" :geometry="{ label: { x: 0.76, y: 0.47, width: 0.36 } }" />

```kotlin
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
  embeddedServer(
    Netty,
    environment = applicationEnvironment {
      config = HoconApplicationConfig(ConfigFactory.load())
    },
    configure = { connector { port = 8080 } },
  ) { module() }.start(wait = true)
}
```

<!--
`ConfigFactory.load()` is Typesafe Config's default: `application.conf`
from the classpath, merged with system properties. Wrapped in
`HoconApplicationConfig`, it is the same object `EngineMain` would have
built, minus the part where Ktor reads `ktor.deployment.port` for you.
For the command line, `CommandLineConfig(args)` parses the same flags
`EngineMain` accepts: `configure = { takeFrom(CommandLineConfig(args).engineConfig) }`
brings `-port=8080` to an embedded server.
-->

---

# Hoplite maps files to your own type

> Keep `ktor { }` for Ktor; your settings get a data class

<DrawnAnnotation text="data class Config" label="Plain Kotlin, no Ktor type: pass it to whatever needs it" :geometry="{ label: { x: 0.76, y: 0.352, width: 0.36 } }" />
<DrawnAnnotation text="loadConfigOrThrow<Config>()" label="Missing or mistyped keys fail at start-up, every error listed" :geometry="{ label: { x: 0.66, y: 0.54, width: 0.42 } }" />

```kotlin
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

data class Config(val name: String, val version: String)

val config: Config = ConfigLoaderBuilder.default()
  .addResourceSource("/defaults.conf")
  .build()
  .loadConfigOrThrow<Config>()
```

<!--
The alternative to threading `environment.config` through the code:
give the application's own settings a type. Hoplite
(`com.sksamuel.hoplite:hoplite-hocon`, plus `-yaml` or `-json` for other
formats) decodes a file into a data class by parameter name, nested
classes for nested sections, `Duration`, enums and `List` included. The
Ktor environment keeps the Ktor keys; a `Config` value goes wherever a
constructor or a `context` parameter wants it. `loadConfigOrThrow`
reports every problem at once, not the first one.
-->

---
magic-move
---

# Hoplite maps files to your own type

> Keep `ktor { }` for Ktor; your settings get a data class

<DrawnAnnotation text="&quot;/prod.conf&quot;" label="Sources stack: the first file wins, the later ones fill the gaps" :geometry="{ label: { x: 0.78, y: 0.446, width: 0.34 } }" />

```kotlin
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

data class Config(val name: String, val version: String)

val config: Config = ConfigLoaderBuilder.default()
  .addResourceSource("/prod.conf")
  .addResourceSource("/defaults.conf")
  .build()
  .loadConfigOrThrow<Config>()
```

<!--
Layering is the reason to reach for Hoplite over a plain
`kotlinx.serialization` decoder. Sources are consulted in order, so a
small `prod.conf` overrides only what differs from `defaults.conf`, and
`addEnvironmentSource()` or `addCommandLineSource(args)` slot in at the
front. Preprocessors resolve `${env}` references and, with the extra
modules, secrets from AWS Secrets Manager, Azure Key Vault or Vault.
-->

---

# Secrets never live in the source

> Not in git, not in the image, not in a log line

<DrawnAnnotation text="&quot;supersecret&quot;" label="In every clone, every build, every image layer" color="red" :geometry="{ label: { x: 0.62, y: 0.305, width: 0.36 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

val secret = "supersecret"

val token: String = JWT.create().sign(Algorithm.HMAC256(secret))
```

<!--
Every lesson so far hard-coded something for the sake of the slide, and
lesson 8 marked this one in red already. A secret in the source is in
every clone of the repository, every CI log that prints the build, and
every layer of the image: rotating it means a release. The place for a
secret is the environment of the process, filled by whoever runs it, or
a vault the application asks at start-up. And be careful what the
application logs: a `toString` of a config class, or a debug line of a
plug-in, can print the secret into the very logs everyone can read.
-->

---
magic-move
---

# Secrets never live in the source

> Not in git, not in the image, not in a log line

<DrawnAnnotation text="property(&quot;jwt.secret&quot;)" />
<DrawnAnnotation text="${JWT_SECRET}" label="Set by the platform: a vault, a Kubernetes secret, the CI's variables" :geometry="{ label: { x: 0.68, y: 0.62, width: 0.42 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application

fun Application.module() {
  val secret = environment.config.property("jwt.secret").getString()
  val token: String = JWT.create().sign(Algorithm.HMAC256(secret))
}
```

```text
jwt {
  secret = ${JWT_SECRET}
}
```

<!--
The fix is the lookup from a few slides ago and a key without a default:
`${JWT_SECRET}` with no question mark, so a container started without
the variable refuses to start instead of signing tokens with an empty
string. Cloud providers offer vaults and inject their values as
environment variables or mounted files; either way the code only ever
sees `jwt.secret`.
-->

---

# A proxy terminates TLS

<DrawnAnnotation text="X-Forwarded-For: 203.0.113.9" label="Added by the proxy: who really called, and over which scheme" :geometry="{ label: { x: 0.675, y: 0.242, width: 0.55 } }" />
<DrawnAnnotation text="install(XForwardedHeaders)" label="Only behind your own proxy: a client can send these headers too" color="red" :geometry="{ label: { x: 0.69, y: 0.42, width: 0.58 } }" />
<DrawnAnnotation text="call.request.origin" label="The client as the proxy saw it; `call.request.local` is the proxy" :geometry="{ label: { x: 0.74, y: 0.514, width: 0.48 } }" />

```http
GET /whoami HTTP/1.1
X-Forwarded-For: 203.0.113.9
X-Forwarded-Proto: https
```

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  install(XForwardedHeaders)
  routing {
    get("/whoami") {
      val origin = call.request.origin
      call.respondText("${origin.remoteHost} over ${origin.scheme}")
    }
  }
}
```

<!--
Lesson 8 said "only over TLS" four times and never showed a certificate:
in a container the certificate is the platform's problem. The ingress,
the load balancer or nginx terminates TLS and forwards plain HTTP to
`8080`, so without this plug-in every request looks like it came from
the proxy over `http`. `ktor-server-forwarded-header` has two plug-ins:
`XForwardedHeaders` reads the de facto headers,
`ForwardedHeaders` the RFC 7239 one, `useFirstProxy()` and friends pick
the right hop when there are several. `HttpsRedirect` and `HSTS` decide
on `origin.scheme`, so they only work with this installed first. For
the rare server that terminates TLS itself, `sslConnector { }` with a
key store, or `ktor.security.ssl` in the file, is the other half.
-->

---

# The server stops in two phases

<DrawnAnnotation text="monitor.subscribe(ApplicationStopping)" label="On `SIGTERM`, before connections close: flush a queue, tell the balancer" :geometry="{ label: { x: 0.575, y: 0.336, width: 0.75 } }" />
<DrawnAnnotation text="shutdownGracePeriod = 5000" label="No new connections; requests in flight may finish" :geometry="{ label: { x: 0.695, y: 0.562, width: 0.55 } }" />
<DrawnAnnotation text="shutdownTimeout = 10000" label="Then the engine stops waiting" :geometry="{ label: { x: 0.65, y: 0.61, width: 0.5 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log

fun Application.module() {
  monitor.subscribe(ApplicationStarted) { log.info("Ready") }
  monitor.subscribe(ApplicationStopping) { log.info("Draining requests") }
  routes()
}
```

```text
ktor {
  deployment {
    shutdownGracePeriod = 5000
    shutdownTimeout = 10000
  }
}
```

<!--
`docker stop` and a Kubernetes rollout send `SIGTERM` and wait, ten
seconds by default, before they kill. Ktor answers in two phases: the
grace period, during which no new connection is accepted and the
requests in flight run to completion, and the time-out after which the
remaining ones are cancelled, which is the `CancellationException`
lesson 6's WebSocket handler caught. `monitor` is the application's
event bus: `ApplicationStarted`, `ApplicationStopPreparing`,
`ApplicationStopping`, `ApplicationStopped`; lesson 5's DI plug-in
closes its `AutoCloseable`s on the last one. From code,
`server.stop(gracePeriodMillis, timeoutMillis)` does the same as the
two keys.
-->

---

# One application, three packages

| A container     | `./gradlew buildImage`: a JRE and the fat JAR in one image, for any cloud       |
|-----------------|---------------------------------------------------------------------------------|
| A servlet       | `./gradlew war`: a WAR archive for a Tomcat or Jetty the organisation already runs |
| A native binary | GraalVM `native-image` on `CIO`: no JVM to start, reflection listed in a file    |

<!--
Deployment starts where configuration ends: the same JAR, with the port
and the secrets coming from outside, has to reach a machine. Containers
are the common case, and the rest of the lesson is mostly that. The WAR
exists for teams with a servlet container in place. The native image is
the one for fast start-up and small memory, at the price of a GraalVM
toolchain and a `reflect-config.json` for everything Ktor reflects on;
the `ktor-samples` repository has a working `graalvm` project, and
Kotlin/Native builds CIO servers too.
-->

---

# The Ktor Gradle plug-in packages the server

<DrawnAnnotation text="id(&quot;io.ktor.plugin&quot;) version &quot;3.6.0&quot;" label="Fat JAR, Docker image, OpenAPI: one plug-in for the whole life cycle" :geometry="{ label: { x: 0.72, y: 0.383, width: 0.42 } }" />
<DrawnAnnotation text="&quot;com.example.ApplicationKt&quot;" label="The file holding `main`: `Application.kt` compiles to `ApplicationKt`" :geometry="{ label: { x: 0.74, y: 0.571, width: 0.42 } }" />

```kotlin gradle no-compile
plugins {
  kotlin("jvm") version "2.4.20"
  kotlin("plugin.serialization") version "2.4.20"
  application
  id("io.ktor.plugin") version "3.6.0"
}

application {
  mainClass.set("com.example.ApplicationKt")
}
```

<!--
The plug-in applies Gradle's `application` plug-in and adds its own
tasks on top; `start.ktor.io` projects have it already. It needs to know
the entry point, the class holding `main`, which is the same `FileKt`
naming as the module reference in `application.conf`. The plug-in also
adds the Ktor BOM, so dependencies can drop their version, and the
`openApi { }` block of lesson 6 lives in the same `ktor { }` extension.
-->

---

# Gradle builds the image, Docker runs it

<DrawnAnnotation text="buildFatJar" label="Every dependency in one JAR: what the image will contain" :geometry="{ label: { x: 0.7, y: 0.195, width: 0.4 } }" />
<DrawnAnnotation text="jib-image.tar" label="Jib writes the image without a daemon or a `Dockerfile`" :geometry="{ label: { x: 0.7, y: 0.383, width: 0.4 } }" />

```bash
./gradlew buildFatJar
java -jar build/libs/greeting-service-all.jar

./gradlew buildImage
docker load < build/jib-image.tar
```

<!--
`buildFatJar` is the JVM-only deliverable: one archive with the
application and every dependency, started with `java -jar`; `EngineMain`
inside reads `application.conf` and honours `-port=`. `buildImage` puts
that JAR on a JRE base image using Jib, so the build machine needs no
Docker installed and no `Dockerfile` written; the result is a tarball
that `docker load` imports. A hand-written multi-stage `Dockerfile` is
the alternative when the image needs more than a JRE.
-->

---
magic-move
---

# Gradle builds the image, Docker runs it

<DrawnAnnotation text="runDocker" label="Builds, loads, and starts the container on `8080`" :geometry="{ label: { x: 0.68, y: 0.477, width: 0.36 } }" />
<DrawnAnnotation text="publishImage" label="To Docker Hub or Google: `externalRegistry` in `ktor { docker { } }`" :geometry="{ label: { x: 0.68, y: 0.585, width: 0.42 } }" />

```bash
./gradlew buildFatJar
java -jar build/libs/greeting-service-all.jar

./gradlew buildImage
docker load < build/jib-image.tar

./gradlew runDocker
./gradlew publishImageToLocalRegistry
./gradlew publishImage
```

<!--
The remaining tasks talk to a Docker daemon. `runDocker` is the
developer loop: build the image straight into the daemon and start it,
the server answers on `http://0.0.0.0:8080` with the default port
mapping. `publishImageToLocalRegistry` leaves the image in the local
daemon for `docker run` or a Compose file; `publishImage` pushes to a
registry named in the plug-in configuration, with the credentials read
from environment variables, never from the build script.
-->

---

# The image is configured in Gradle

<DrawnAnnotation text="JavaVersion.VERSION_21" label="The base image: a JRE, not a JDK" :geometry="{ label: { x: 0.72, y: 0.289, width: 0.3 } }" />
<DrawnAnnotation text="&quot;greeting-service&quot;" label="`greeting-service:0.0.1` instead of `ktor-docker-image:latest`" :geometry="{ label: { x: 0.72, y: 0.375, width: 0.4 } }" />
<DrawnAnnotation text="DockerPortMapping(" label="Host port `80` to container port `8080` when `runDocker` starts it" :geometry="{ label: { x: 0.72, y: 0.64, width: 0.4 } }" />

```kotlin gradle no-compile
ktor {
  docker {
    jreVersion.set(JavaVersion.VERSION_21)
    localImageName.set("greeting-service")
    imageTag.set("0.0.1")
    portMappings.set(
      listOf(
        io.ktor.plugin.features.DockerPortMapping(
          80, 8080, io.ktor.plugin.features.DockerPortMappingProtocol.TCP,
        ),
      ),
    )
  }
}
```

<!--
Everything about the image is a property of the `docker` extension.
`jreVersion` picks the Eclipse Temurin JRE the layers are built on;
`localImageName` and `imageTag` replace the `ktor-docker-image:latest`
default. `portMappings` only matters for `runDocker`: the container still
listens on whatever `PORT` or `application.conf` says, this maps a host
port to it. `externalRegistry.set(DockerImageRegistry.dockerHub(...))`
with `providers.environmentVariable` for the credentials is what
`publishImage` needs.
-->

---

# A servlet container runs the WAR

<DrawnAnnotation text="war" label="Gradle's own plug-in: `./gradlew war` writes `build/libs/*.war`" :geometry="{ label: { x: 0.62, y: 0.289, width: 0.42 } }" />
<DrawnAnnotation text="ktor-server-servlet-jakarta" label="Instead of `ktor-server-netty`: the container owns the connections" :geometry="{ label: { x: 0.7, y: 0.56, width: 0.44 } }" />

```kotlin gradle no-compile
plugins {
  kotlin("jvm") version "2.4.20"
  war
}

dependencies {
  implementation("io.ktor:ktor-server-servlet-jakarta:3.6.0")
}
```

<!--
A servlet is the JVM's oldest server API: Tomcat, Jetty and the
application servers run one or many of them, each packaged as a WAR, a
JAR with a `WEB-INF` directory. Ktor ships a fifth engine for this
world, and it replaces the engine dependency rather than adding to it:
the container accepts the connections and hands requests to Ktor, so
`ktor.deployment.port` and the SSL settings in `application.conf` are
ignored. The `war` task comes from Gradle itself; the Gretty plug-in
(`org.gretty`) runs the WAR on an embedded Tomcat or Jetty during
development.
-->

---
class: servlet-slide
---

# `web.xml` points the container at Ktor

<DrawnAnnotation text="ServletApplicationEngine" label="The fifth engine: the container calls it, nobody calls `start`" :geometry="{ label: { x: 0.77, y: 0.345, width: 0.34 } }" />
<DrawnAnnotation text="application.conf" label="Where the modules are listed; the container decides the port" :geometry="{ label: { x: 0.74, y: 0.50, width: 0.4 } }" />
<DrawnAnnotation text="<url-pattern>/</url-pattern>" label="The context path: one container, several servlets, each on its own path" :geometry="{ label: { x: 0.7, y: 0.657, width: 0.42 } }" />

```xml
<servlet>
  <servlet-name>KtorServlet</servlet-name>
  <servlet-class>io.ktor.server.servlet.jakarta.ServletApplicationEngine</servlet-class>
  <init-param>
    <param-name>io.ktor.ktor.config</param-name>
    <param-value>application.conf</param-value>
  </init-param>
  <async-supported>true</async-supported>
</servlet>
<servlet-mapping>
  <servlet-name>KtorServlet</servlet-name>
  <url-pattern>/</url-pattern>
</servlet-mapping>
```

<!--
`src/main/webapp/WEB-INF/web.xml` is the deployment descriptor every
servlet container reads. It names the servlet class, which for Ktor is
always `ServletApplicationEngine`, tells it which configuration file
holds the modules, and asks for asynchronous support, without which the
coroutines would block a container thread per request. The mapping is
the path prefix the container routes to Ktor; `ktor.deployment.rootPath`
in the file should match it. Copy the file from the docs, deploy the WAR
into `webapps`, and the container does the rest.
-->

---

# The code stays the same from laptop to cloud

- `embeddedServer(Netty, configure = { })` → configuration in code
- `EngineMain` + `application.conf` / `application.yaml` → a file
- `property("…")`, `${?PORT}` → keys from the environment
- `loadConfigOrThrow<Config>()` → Hoplite, your own type
- `XForwardedHeaders`, `ApplicationStopping`, `shutdownGracePeriod` → a proxy in front, a clean stop
- `io.ktor.plugin` → `buildImage`, `runDocker`; `war` → a servlet container

> **Configuration comes from outside the JAR.**
>
> The same image runs on every port, with every secret.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Ship the greeting service as a container

- Move the port and the JWT secret to `application.yaml`
- Read them in the module with `environment.config`
- Build the image with `./gradlew buildImage`
- Run it with `./gradlew runDocker`
- Start it again with a different `PORT`
