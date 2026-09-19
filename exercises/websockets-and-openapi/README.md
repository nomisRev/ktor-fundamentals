# Echo JSON and publish the spec

The exercises are in `src/chat/Chat.kt` (the socket), `src/chat/Greeting.kt`
(the documented route) and `src/chat/Docs.kt` (Swagger UI and the hidden
health check). Check them with
`./kotlin test --include-module websockets-and-openapi` from `exercises/`.

The steps that are not code:

1. Run the module in the project of lesson 1 and open `chat.http` in
   IntelliJ IDEA: the `WEBSOCKET ws://localhost:8080/chat` request sends one
   JSON frame per block and shows every echo, timestamp included.
2. The slides document the route with a KDoc comment on `get("/greet/{name}")`
   (`Path: name [String] ...`, `Response: 200 [GreetingResponse] ...`), which
   the Ktor Gradle plug-in turns into the document at build time:

   ```kotlin
   plugins { id("io.ktor.plugin") version "3.5.2" }
   ktor { openApi { enabled = true; codeInferenceEnabled = true } }
   ```

   The Toolchain project has no Gradle plug-in, so the exercise documents the
   route at runtime with `.describe { }` instead; both end up in the same
   document. Try the KDoc variant in the Gradle project.
3. Open `http://localhost:8080/swagger`, expand `GET /greet/{name}` and call
   it from the browser with `Try it out`. `/health` still answers, but it is
   not in the list.
