# Ship the greeting service as a container

Nothing on this exercise slide is code: it happens in the Gradle project of
lesson 1, with the configuration file of lesson 2 in `src/main/resources`.

1. Apply the Ktor Gradle plug-in next to `application` and name the image:

   ```kotlin
   plugins {
     kotlin("jvm") version "2.4.20"
     kotlin("plugin.serialization") version "2.4.20"
     application
     id("io.ktor.plugin") version "3.6.0"
   }

   application {
     mainClass.set("com.example.ApplicationKt")
   }

   ktor {
     docker {
       localImageName.set("greeting-service")
       imageTag.set("0.0.1")
     }
   }
   ```

2. `./gradlew buildImage` builds the image with Jib; no Dockerfile.
3. `./gradlew runDocker` runs it and maps the port.
4. Start it again with a different port:
   `docker run -e PORT=9090 -p 9090:9090 greeting-service:0.0.1`. The log says
   `Responding at http://0.0.0.0:9090` because `"$PORT:8080"` in
   `application.yaml` prefers the variable over the fallback.
