# Build and run your first server

The steps of the exercise slide that are not code:

1. Generate a project at [start.ktor.io](https://start.ktor.io) (Ktor 3.5.2,
   Gradle Kotlin DSL, engine Netty, no plug-ins yet) and open it in IntelliJ
   IDEA.
2. Run it with `./gradlew run` and open `http://0.0.0.0:8080` in the browser.
   The log line `Responding at http://0.0.0.0:8080` tells you where it listens.
3. Copy `src/firstserver/Application.kt` from this module into the generated
   project once the tests here pass, and run it again.

The code steps are the exercises in `src/firstserver/Application.kt`; check
them with `./kotlin test --include-module first-server` from `exercises/`.
