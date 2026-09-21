# Ship the greeting service as a container

Two exercises are code: `resources/application.yaml` (the port and the
secret; in a Gradle project the file is `src/main/resources/application.yaml`)
and `settings()` in `src/configuration/Application.kt`. Check them with
`./kotlin test --include-module configuration` from `exercises/`.

The rest of the slide happens in the Gradle project of lesson 1, with the
Ktor Gradle plug-in and `EngineMain` as the main class:

```kotlin
plugins {
  application
  id("io.ktor.plugin") version "3.6.0"
}

application {
  mainClass.set("configuration.ApplicationKt")
}

ktor {
  docker {
    localImageName.set("greeting-service")
    imageTag.set("0.0.1")
  }
}
```

1. `./gradlew buildImage` builds the image with Jib; no Dockerfile.
2. `./gradlew runDocker` runs it and maps the port.
3. Start it again with a different port:
   `docker run -e PORT=9090 -p 9090:9090 greeting-service:0.0.1`. The log says
   `Responding at http://0.0.0.0:9090` because `"$PORT:8080"` in
   `application.yaml` prefers the variable over the default.
