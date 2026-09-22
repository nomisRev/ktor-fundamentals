---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 10</div>

# Deployment

## From a JAR to a container

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
`openApi { }` block of lesson 7 lives in the same `ktor { }` extension.
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
Lesson 9 said "only over TLS" four times and never showed a certificate:
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
lesson 7's WebSocket handler caught. `monitor` is the application's
event bus: `ApplicationStarted`, `ApplicationStopPreparing`,
`ApplicationStopping`, `ApplicationStopped`; lesson 6's DI plug-in
closes its `AutoCloseable`s on the last one. From code,
`server.stop(gracePeriodMillis, timeoutMillis)` does the same as the
two keys.
-->

---

# The code stays the same from laptop to cloud

- `io.ktor.plugin` → `buildFatJar`, `buildImage`, `runDocker`, `publishImage`
- `ktor { docker { } }` → the JRE, the image name, the port mapping
- `war` + `ktor-server-servlet-jakarta` → a servlet container, `web.xml` points at Ktor
- `XForwardedHeaders` → the client as the proxy saw it, `HTTPS` included
- `ApplicationStopping`, `shutdownGracePeriod` → a clean stop on `SIGTERM`

> **The image is built once; lesson 2's file decides the rest.**
>
> The same image runs on every port, with every secret.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Ship the greeting service as a container

- Apply `io.ktor.plugin` and name the image in `ktor { docker { } }`
- Build the image with `./gradlew buildImage`
- Run it with `./gradlew runDocker`
- Start it again with a different `PORT` and read the log line
