---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 3</div>

# Type-safe routing and HTML

## Routes as classes, pages as code

---

# The route is the last untyped thing

> Nullable everything, parsed by hand, checked by nobody

<DrawnAnnotation text="&quot;/hello/{name}/{hour}&quot;" label="A typo here is found by a client, not by the compiler" color="red" :geometry="{ label: { x: 0.72, y: 0.53, width: 0.4 } }" />
<DrawnAnnotation text="call.parameters[&quot;hour&quot;]?.toInt()" label="Manual parsing: a bad `hour` is a `NumberFormatException`, so a `500`" color="red" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.module() {
  routing {
    get("/bye/{name}") {
      call.respondText("Bye, ${call.parameters["name"]}")
    }
    get("/hello/{name}/{hour}") {
      val name = call.parameters["name"]
      val hour = call.parameters["hour"]?.toInt()
      call.respondText("Hello, $name, it's $hour o'clock")
    }
  }
}
```

<!--
This is the handler from the previous exercise. Every parameter arrives as a
`String?`, every conversion is our job, and nothing relates the pattern in
the string to the names we read out of `call.parameters`. Think of every
route that starts with `/product`: one typo and the client gets a `404`.
-->

---

# A route is a class

<DrawnAnnotation text="@Resource(&quot;/bye/{name}&quot;)" label="The path pattern lives on the class, apart from any handler" :geometry="{ label: { x: 0.74, y: 0.31, width: 0.4 } }" />
<DrawnAnnotation text="@Serializable" label="Same machinery as the body: the parameters are deserialized" :geometry="{ label: { x: 0.6, y: 0.195, width: 0.4 } }" />

```kotlin
import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/bye/{name}")
class Bye(val name: String)
```

<!--
`ktor-resources` is a small multiplatform artifact: the same class can
describe the route on the server and build the URL on the client, which
lesson 5 uses. The annotation is the only place the path is spelled out.
-->

---
magic-move
---

# A route is a class

<DrawnAnnotation text="{hour}" label="Field names match the captures" :geometry="{ label: { x: 0.64, y: 0.43, width: 0.3 } }" />
<DrawnAnnotation text="val hour: Int" label="Not a `String`: converted, and a `400` when it cannot be" :geometry="{ label: { x: 0.66, y: 0.53, width: 0.42 } }" />

```kotlin
import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/bye/{name}")
class Bye(val name: String)

@Serializable
@Resource("/hello/{name}/{hour}")
class Hello(
  val name: String,
  val hour: Int,
)
```

<!--
Primitive types, enums and nullable versions of them can appear in a path;
anything more structured belongs in the body. A request whose `hour` does
not parse never reaches the handler.
-->

---
magic-move
---

# A route is a class

<DrawnAnnotation text="val lang: String? = &quot;en&quot;" label="Not in the pattern: a query parameter, optional thanks to the default" :geometry="{ label: { x: 0.68, y: 0.58, width: 0.42 } }" />

```kotlin
import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/bye/{name}")
class Bye(val name: String)

@Serializable
@Resource("/hello/{name}/{hour}")
class Hello(
  val name: String,
  val hour: Int,
  val lang: String? = "en",
)
```

<!--
`/hello/Alex/9?lang=nl` fills `lang`; `/hello/Alex/9` leaves the default. A
non-nullable field without default is a required query parameter.
-->

---
magic-move
---

# A route is a class

<DrawnAnnotation text="class Greeting" label="Nested classes are nested paths: `/greet/{name}/hello/{hour}`" :geometry="{ label: { x: 0.7, y: 0.243, width: 0.45 } }" />
<DrawnAnnotation text="val parent: Greeting" label="The outer route is a field, so its parameters are in reach" :geometry="{ label: { x: 0.72, y: 0.48, width: 0.42 } }" />

```kotlin
import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/greet/{name}")
class Greeting(val name: String, val lang: String? = "en") {
  @Serializable
  @Resource("bye")
  class Bye(val parent: Greeting)

  @Serializable
  @Resource("hello/{hour}")
  class Hello(val parent: Greeting, val hour: Int)
}
```

<!--
The inner pattern is relative to the outer one, and `parent` has to be
declared explicitly: nothing in Kotlin ties a nested class to an instance
of the outer one. This `Greeting` is the class the rest of the deck uses.
-->

---

# The plug-in does the matching

<DrawnAnnotation text="install(Resources)" label="`ktor-server-resources`; note the different `get` import" :geometry="{ label: { x: 0.64, y: 0.243, width: 0.42 } }" />
<DrawnAnnotation text="get<Greeting.Bye>" label="The class is the route" />
<DrawnAnnotation text="req.parent.name" label="`req` is the parsed instance: no `call.parameters`" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.module() {
  install(Resources)
  routing {
    get<Greeting.Bye> { req ->
      call.respondText("Bye, ${req.parent.name}")
    }
  }
}
```

<!--
`io.ktor.server.resources.get` is an overload of the `get` from lesson 1
that takes a reified type instead of a string. IntelliJ IDEA offers both;
pick the wrong one and the type parameter does not compile.
-->

---
magic-move
---

# The plug-in does the matching

<DrawnAnnotation text="req.hour" label="An `Int`, already" :geometry="{ label: { x: 0.6, y: 0.66, width: 0.3 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.module() {
  install(Resources)
  routing {
    get<Greeting.Bye> { req ->
      call.respondText("Bye, ${req.parent.name}")
    }
    get<Greeting.Hello> { req ->
      call.respondText("Hello, ${req.parent.name}, it's ${req.hour} o'clock")
    }
  }
}
```

<!--
Type-safe all the way: `Resources` for the route, `ContentNegotiation` for
the body. The handler only ever sees Kotlin objects.
-->

---

# Routes are extensions of `Route`

<DrawnAnnotation text="byeRoutes()" label="One call per group of routes, one file per group" :geometry="{ label: { x: 0.55, y: 0.34, width: 0.4 } }" />
<DrawnAnnotation text="fun Route.byeRoutes()" label="An extension of `Route`: the same DSL, from anywhere" :geometry="{ label: { x: 0.7, y: 0.53, width: 0.42 } }" />
<DrawnAnnotation text="delete<Greeting.Bye>" label="More than one route per function" />

<TypeHint :line="3" receiver="Routing">
<TypeHint :line="10" receiver="RoutingContext">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing

fun Route.helloRoutes() {
  get<Greeting.Hello> { req ->
    call.respondText("Hello, ${req.parent.name}, it's ${req.hour} o'clock")
  }
}

// Example
fun Application.module() {
  install(Resources)
  routing {
    byeRoutes()
    helloRoutes()
  }
}

fun Route.byeRoutes() {
  get<Greeting.Bye> { req ->
    call.respondText("Bye, ${req.parent.name}")
  }
  delete<Greeting.Bye> { req ->
    call.respondText("I don't like you either, ${req.parent.name}")
  }
}
```

</TypeHint>
</TypeHint>

<!--
One big `routing` block does not scale. `routing { }` passes a `Route`
receiver to its block, so any `Route.() -> Unit` function slots in; group
them by feature and keep `module()` as the table of contents.
-->

---

# Anything that responds suspends

<InlineCompilerError :line="12" text="respondText" message="Suspend function 'suspend fun ApplicationCall.respondText(text: String, contentType: ContentType? = ..., status: HttpStatusCode? = ..., configure: OutgoingContent.() -> Unit = ...): Unit'\ncan only be called from a coroutine or another suspend function." style="--inline-compiler-error-message-size: 1.05rem">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.routing

fun Application.module() {
  install(Resources)
  routing {
    get<Greeting.Bye> { bye(it) }
    get<Greeting.Hello> { req ->
      call.respondText("Hello, ${req.parent.name}, it's ${req.hour} o'clock")
    }
  }
}

fun RoutingContext.bye(req: Greeting.Bye) {
  call.respondText("Bye, ${req.parent.name}")
}
```

</InlineCompilerError>

<!--
The handler lambda is a `suspend` lambda; a plain function is not. Moving
the body out of the lambda loses that, and the first `respond` tells you.
-->

---
magic-move
---

# Handlers are extensions of `RoutingContext`

<DrawnAnnotation text="RoutingContext" label="Where `call` comes from: the receiver of every handler" :geometry="{ label: { x: 0.72, y: 0.53, width: 0.42 } }" />
<DrawnAnnotation text="suspend" label="Handlers suspend, and so does anything that responds" :geometry="{ label: { x: 0.74, y: 0.62, width: 0.4 } }" />

<TypeHint :line="3" receiver="Routing">
<TypeHint :line="4" receiver="RoutingContext">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.routing

fun Application.module() {
  install(Resources)
  routing {
    get<Greeting.Bye> { bye(it) }
    get<Greeting.Hello> { hello(it) }
  }
}

suspend fun RoutingContext.bye(req: Greeting.Bye) {
  call.respondText("Bye, ${req.parent.name}")
}

suspend fun RoutingContext.hello(req: Greeting.Hello) {
  call.respondText("Hello, ${req.parent.name}, it's ${req.hour} o'clock")
}
```

</TypeHint>
</TypeHint>

<!--
The other way to split: keep the routes together and move the code inside
each of them out. The handler lambda runs with a `RoutingContext` receiver,
which carries `call`; an extension of it can do everything the lambda can.
Forget `suspend` and `respondText` does not compile.
-->

---
magic-move
---

# The logic does not need Ktor at all

<DrawnAnnotation text="fun bye(req: Greeting.Bye): String" label="No `call`, no `suspend`: a plain function, trivial to test" :geometry="{ label: { x: 0.78, y: 0.665, width: 0.38 } }" />
<DrawnAnnotation text="call.respondText(bye(it))" label="Ktor stays in the route" :geometry="{ label: { x: 0.8, y: 0.34, width: 0.3 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.module() {
  install(Resources)
  routing {
    get<Greeting.Bye> { call.respondText(bye(it)) }
    get<Greeting.Hello> { call.respondText(hello(it)) }
  }
}

fun bye(req: Greeting.Bye): String = "Bye, ${req.parent.name}"

fun hello(req: Greeting.Hello): String =
  "Hello, ${req.parent.name}, it's ${req.hour} o'clock"
```

<!--
The dependency on the framework shrinks to one line per route. Lesson 7
tests the routes through the test host; functions like these are tested
with nothing at all.
-->

---

# HTML is just a string, until it is not

> Template engines exist: Mustache, JTE, FreeMarker, Thymeleaf. Or write HTML as code.

<DrawnAnnotation text="${req.parent.name}" label="`name` = `<script>…</script>`: injection" color="red" :geometry="{ label: { x: 0.83, y: 0.41, width: 0.32 } }" />
<DrawnAnnotation text="ContentType.Text.Html" label="Right header, wrong tool" :geometry="{ label: { x: 0.72, y: 0.53, width: 0.3 } }" />

```kotlin
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.hello(req: Greeting.Hello) {
  val response = "<h1>Hello, ${req.parent.name}!</h1>"
  call.respondText(response, contentType = ContentType.Text.Html)
}
```

<!--
Building markup by hand is hard to read, hard to maintain, and unsafe:
whatever the client puts in `name` ends up in the page unescaped. Ktor has
plug-ins for the usual template engines; `kotlinx.html` is the option that
keeps the page in Kotlin.
-->

---

# HTML is a Kotlin DSL

<DrawnAnnotation text="respondHtml" label="`ktor-server-html-builder`: `Content-Type: text/html`, status `200`" :geometry="{ label: { x: 0.66, y: 0.243, width: 0.42 } }" />

<TypeHint :line="2" receiver="HTML">

```kotlin
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*

suspend fun RoutingContext.hello(req: Greeting.Hello) {
  call.respondHtml {
    head {
      title { +"World Greeting Service" }
    }
    body {
      h1 { +"Hello, ${req.parent.name}!" }
    }
  }
}
```

</TypeHint>

<!--
`kotlinx.html` is a library from JetBrains that describes HTML documents as
Kotlin code: one function per tag, blocks for nesting. `respondHtml` builds
the document and sends it with the right content type.
-->

---
magic-move
---

# HTML is a Kotlin DSL

<DrawnAnnotation text="h1 {" label="Nesting is nesting: a block per tag" :geometry="{ label: { x: 0.78, y: 0.48, width: 0.32 } }" />

<TypeHint :line="3" receiver="HEAD">
<TypeHint :line="6" receiver="BODY">

```kotlin
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*

suspend fun RoutingContext.hello(req: Greeting.Hello) {
  call.respondHtml {
    head {
      title { +"World Greeting Service" }
    }
    body {
      h1 { +"Hello, ${req.parent.name}!" }
    }
  }
}
```

</TypeHint>
</TypeHint>

---
magic-move
---

# HTML is a Kotlin DSL

<DrawnAnnotation text="+&quot;Hello, ${req.parent.name}!&quot;" label="`+` appends text, escaped: `<` becomes `&amp;lt;`" :geometry="{ label: { x: 0.72, y: 0.58, width: 0.4 } }" />

```kotlin
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*

suspend fun RoutingContext.hello(req: Greeting.Hello) {
  call.respondHtml {
    head {
      title { +"World Greeting Service" }
    }
    body {
      h1 { +"Hello, ${req.parent.name}!" }
    }
  }
}
```

<!--
The unary plus is an operator on `String` inside a tag: it adds a text
node. Escaping is not optional and not something to remember, which is the
whole answer to the injection slide.
-->

---

# Control flow is plain Kotlin

<DrawnAnnotation text="if (req.name.isEmpty())" label="A conditional, not a template directive" :geometry="{ label: { x: 0.7, y: 0.39, width: 0.36 } }" />
<DrawnAnnotation text="forEach" label="Iteration: `G - I - V - E - N`" :geometry="{ label: { x: 0.55, y: 0.63, width: 0.3 } }" />

```kotlin
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*

suspend fun RoutingContext.spell(req: Spell) {
  call.respondHtml {
    body {
      h1 {
        if (req.name.isEmpty()) {
          +"No name"
        } else {
          +"${req.name.first().uppercaseChar()}"
          req.name.drop(1).forEach { +" - ${it.uppercaseChar()}" }
        }
      }
    }
  }
}
```

<!--
`Spell` is `@Resource("/spell/{name}")`. Conditionals and loops are the
language's own: no `{{#if}}`, no `<c:forEach>`, and the IDE understands all
of it.
-->

---

# Templates are functions

<DrawnAnnotation text="notEmpty(req.name, emptyMessage = &quot;No name&quot;)" label="The check moved out; only the happy path stays" :geometry="{ label: { x: 0.7, y: 0.34, width: 0.36 } }" />

```kotlin
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*

fun FlowContent.notEmpty(
  value: String,
  emptyMessage: String,
  content: FlowContent.() -> Unit,
) {
  if (value.isEmpty()) +emptyMessage else content()
}

// Example
suspend fun RoutingContext.spell(req: Spell) {
  call.respondHtml {
    body {
      h1 {
        notEmpty(req.name, emptyMessage = "No name") {
          +"${req.name.first().uppercaseChar()}"
          req.name.drop(1).forEach { +" - ${it.uppercaseChar()}" }
        }
      }
    }
  }
}
```

<!--
Sub-templates are functions with parameters, like any other function. This
one takes the fallback text and the content to render when there is a name.
-->

---
magic-move
---

# Templates are functions

<DrawnAnnotation text="FlowContent.notEmpty" label="Any tag that holds flow content can call it: `body`, `h1`, `div`" :geometry="{ label: { x: 0.66, y: 0.243, width: 0.44 } }" />
<DrawnAnnotation text="content: FlowContent.() -> Unit" label="The nested document is a function with a receiver" :geometry="{ label: { x: 0.72, y: 0.385, width: 0.42 } }" />
<DrawnAnnotation text="content()" label="Including it is calling it" :geometry="{ label: { x: 0.7, y: 0.48, width: 0.3 } }" />

```kotlin
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*

suspend fun RoutingContext.spell(req: Spell) {
  call.respondHtml {
    body {
      h1 {
        notEmpty(req.name, emptyMessage = "No name") {
          +"${req.name.first().uppercaseChar()}"
          req.name.drop(1).forEach { +" - ${it.uppercaseChar()}" }
        }
      }
    }
  }
}

// Example
fun FlowContent.notEmpty(
  value: String,
  emptyMessage: String,
  content: FlowContent.() -> Unit,
) {
  if (value.isEmpty()) +emptyMessage else content()
}
```

<!--
`FlowContent` is the interface behind every tag that can contain other
tags; `HTML`, `BODY`, `P` are the specific ones. A `FlowContent.() -> Unit`
is a piece of document waiting for a place to be rendered.
-->

---

# A builder is a lambda with a receiver

> `T.() -> Unit`: inside the block, `this` is the builder

| `Application.() -> Unit` | `embeddedServer { }`, a module        |
|--------------------------|---------------------------------------|
| `Route.() -> Unit`       | `routing { }`, `Route.byeRoutes()`    |
| `HTML.() -> Unit`        | `respondHtml { }`                     |
| `FlowContent.() -> Unit` | `body { }`, `h1 { }`, `notEmpty { }` |

<!--
The same shape everywhere in Ktor and in kotlinx.html. A `StringBuilder`
extension that calls `append` a few times is the same pattern one size
smaller: the receiver collects, the block describes. Lesson 1's modules,
this lesson's routes and pages, are all instances of it.
-->

---

# Type-safe all the things

- `@Serializable @Resource("/path/{param}") class` → the route, typed
- `install(Resources)`, `get<T> { req -> }` → matching and conversion
- `Route.() -> Unit`, `RoutingContext.() -> Unit` → routes and handlers in their own files
- `respondHtml { body { h1 { +"…" } } }` → HTML as code, escaped

> **`Resources` for the route, `ContentNegotiation` for the body.**
>
> `kotlinx.html` for the page.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Make the greeting routes type-safe

- Replace the string routes with `@Serializable @Resource` classes
- Nest them: `/greet/{name}/hello/{hour}` with an `Int` hour and an optional `lang`
- Render the greeting as HTML with `respondHtml`, and try a name with `<b>` in it
- Move each handler into a `suspend fun RoutingContext.…` in its own file
