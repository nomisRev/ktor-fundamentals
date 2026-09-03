---
layout: intro
class: section-slide
kodee: welcome
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 1</div>

# Your first Ktor server

## From `main` to a running route

---

# A Ktor server is one expression

```kotlin
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
  embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    routing {
      get("/") {
        call.respondText("Hello world!")
      }
    }
  }.start(wait = true)
}
```
