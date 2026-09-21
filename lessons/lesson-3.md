---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 3</div>

# Typed parameters and HTML

## Parameters as properties, pages as code

---

# Every parameter arrives as a `String?`

> Nullable everything, parsed by hand, checked by nobody

<DrawnAnnotation text="call.parameters[&quot;name&quot;]" label="The key is spelled twice: in the pattern and in the handler" color="red" :geometry="{ label: { x: 0.72, y: 0.43, width: 0.4 } }" />
<DrawnAnnotation text="call.parameters[&quot;hour&quot;]?.toInt()" label="Manual parsing: a bad `hour` is a `NumberFormatException`, so a `500`" color="red" :geometry="{ label: { x: 0.72, y: 0.53, width: 0.4 } }" />

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
`String?`, every conversion is our job, and nothing relates the capture in
the pattern to the key we read out of `call.parameters`: rename one and the
other silently answers `null`.
-->

---
magic-move
---

# The property name is the key

<DrawnAnnotation text="by call.pathParameters" label="Property delegation: `import io.ktor.server.util.getValue`" :geometry="{ label: { x: 0.8, y: 0.34, width: 0.36 } }" />
<DrawnAnnotation text="val name: String" label="Named once, in the pattern; never `null`, a capture is always there" :geometry="{ label: { x: 0.8, y: 0.44, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.module() {
  routing {
    get("/bye/{name}") {
      val name: String by call.pathParameters
      call.respondText("Bye, $name")
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
`Parameters` has a `getValue` operator, so it can sit on the right of `by`:
Kotlin hands it the property, and the delegate looks up `property.name`.
The key is written once, in the pattern. `call.pathParameters` holds the
captures, `call.queryParameters` the query string, `call.parameters` both.
-->

---
magic-move
---

# Ktor converts the type for you

<DrawnAnnotation text="val hour: Int" label="Not a `String`: converted, and a `400 Bad Request` when it cannot be" :geometry="{ label: { x: 0.8, y: 0.58, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.module() {
  routing {
    get("/bye/{name}") {
      val name: String by call.pathParameters
      call.respondText("Bye, $name")
    }
    get("/hello/{name}/{hour}") {
      val name: String by call.pathParameters
      val hour: Int by call.pathParameters
      call.respondText("Hello, $name, it's $hour o'clock")
    }
  }
}
```

<!--
The declared type drives the conversion: `Int`, `Long`, `Double`,
`Boolean`, an enum by constant name, `Uuid` since 3.6.0. A value that does
not parse throws `ParameterConversionException`, a `BadRequestException`,
and Ktor answers `400` before any status page gets involved. The handler
never sees a request whose `hour` is not a number.
-->

---
magic-move
---

# A query parameter is a property too

<DrawnAnnotation text="String? by call.queryParameters" label="Not in the pattern: `?lang=nl`. Nullable, so absent is `null`; non-null would be a `400`" :geometry="{ label: { x: 0.8, y: 0.63, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.module() {
  routing {
    get("/bye/{name}") {
      val name: String by call.pathParameters
      call.respondText("Bye, $name")
    }
    get("/hello/{name}/{hour}") {
      val name: String by call.pathParameters
      val hour: Int by call.pathParameters
      val lang: String? by call.queryParameters
      val hello = if (lang == "nl") "Hallo" else "Hello"
      call.respondText("$hello, $name, it's $hour o'clock")
    }
  }
}
```

<!--
`/hello/Alex/9?lang=nl` fills `lang`; `/hello/Alex/9` leaves it `null`. The
nullability of the property is the optionality of the parameter: declare
`String` and a missing `lang` is a `MissingRequestParameterException`,
again a `400`. Absence and type are both checked before the first line of
your own code runs.
-->

---

# What a parameter can be

| `Int`, `Long`, `Double`, `Boolean`, `Char` | `toInt()` and friends; a failure is a `400`  |
|--------------------------------------------|----------------------------------------------|
| `enum class`                               | By constant name                             |
| `Uuid`                                     | Ktor 3.6.0, `kotlin.uuid`                    |
| `List<T>`                                  | Every value of a repeated key: `?tag=a&tag=b` |
| `T?`                                       | Absent is `null`; a non-null `T` is a `400`  |

<!--
`DefaultConversionService` is the list; anything else is a
`DataConversionException` and a `400` as well. Complex values do not belong
in the path or the query string in the first place: that is what the body
and lesson 2's `receive` are for. Ktor's own `DataConversion` plug-in can
register more converters, but the next slide is the way I prefer. Without
a delegate, `call.requirePathParameter("name")` and
`call.requireQueryParameter("lang")`, Ktor 3.5, are the one-liners that
throw the same `400` instead of handing back a `String?`.
-->

---

# Strong types at the edge

<DrawnAnnotation text="value class Hour" label="A value class instead of a raw `Int`: `0..23` is part of the type" :geometry="{ label: { x: 0.78, y: 0.5, width: 0.4 } }" />
<DrawnAnnotation text="ParameterConversionException" label="Ktor's own exception, so the answer stays a `400`" :geometry="{ label: { x: 0.8, y: 0.61, width: 0.36 } }" />
<DrawnAnnotation text="call.hour(&quot;hour&quot;)" label="The name is spelled twice again" color="red" :geometry="{ label: { x: 0.7, y: 0.29, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.ParameterConversionException
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail

fun Application.module() {
  routing {
    get("/hello/{hour}") {
      val hour = call.hour("hour")
      call.respondText("Hello, it's ${hour.value} o'clock")
    }
  }
}

@JvmInline value class Hour(val value: Int)

fun ApplicationCall.hour(name: String): Hour {
  val raw = parameters.getOrFail<Int>(name)
  if (raw !in 0..23) throw ParameterConversionException(name, "Hour")
  return Hour(raw)
}
```

<!--
An `Int` says nothing about being an hour. A value class costs nothing at
runtime and gives the domain a name the compiler checks everywhere the
value travels. The extension on `ApplicationCall` is plain Ktor inside:
`getOrFail` does the `Int` conversion and the `400` for a missing value;
the range check rethrows Ktor's exception so the status stays right. What
is left is the key, spelled at the call site once more.
-->

---
magic-move
---

# A delegate knows the property name

<DrawnAnnotation text="ReadOnlyProperty<Any?, Hour>" label="The same `by` as Ktor's: Kotlin passes the property, no reflection" :geometry="{ label: { x: 0.78, y: 0.5, width: 0.4 } }" />
<DrawnAnnotation text="prop.name" label="Extend Ktor's syntax for your own domain" :geometry="{ label: { x: 0.8, y: 0.66, width: 0.36 } }" />
<DrawnAnnotation text="val hour by call.hour()" label="Named once again" :geometry="{ label: { x: 0.66, y: 0.29, width: 0.3 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.ParameterConversionException
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import kotlin.properties.ReadOnlyProperty

fun Application.module() {
  routing {
    get("/hello/{hour}") {
      val hour by call.hour()
      call.respondText("Hello, it's ${hour.value} o'clock")
    }
  }
}

@JvmInline value class Hour(val value: Int)

fun ApplicationCall.hour() = ReadOnlyProperty<Any?, Hour> { _, prop ->
  val raw = parameters.getOrFail<Int>(prop.name)
  if (raw !in 0..23) throw ParameterConversionException(prop.name, "Hour")
  Hour(raw)
}
```

<!--
`ReadOnlyProperty` is the interface behind `by`: one `getValue(thisRef,
property)` that Kotlin calls with the `KProperty` of the declaration. The
name comes from the compiler, not from reflection, so this is as cheap as
Ktor's own delegate. One function per domain type, and every route reads
`val hour by call.hour()`, `val postId by call.postId()`.
-->

---

# Routes group by prefix

<DrawnAnnotation text="route(&quot;/greet/{name}&quot;)" label="The prefix is written once; `{name}` is captured for every route inside" :geometry="{ label: { x: 0.66, y: 0.29, width: 0.44 } }" />
<DrawnAnnotation text="get(&quot;hello/{hour}&quot;)" label="Relative to the group: `/greet/{name}/hello/{hour}`" :geometry="{ label: { x: 0.68, y: 0.53, width: 0.42 } }" />

<TypeHint :line="2" receiver="Routing">
<TypeHint :line="3" receiver="Route">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.module() {
  routing {
    route("/greet/{name}") {
      get("bye") {
        val name: String by call.pathParameters
        call.respondText("Bye, $name")
      }
      get("hello/{hour}") {
        val name: String by call.pathParameters
        val hour: Int by call.pathParameters
        call.respondText("Hello, $name, it's $hour o'clock")
      }
    }
  }
}
```

</TypeHint>
</TypeHint>

<!--
Routes are a tree: `route` adds a node, `get` a leaf under it, and the
captures of every node on the way down are in `call.pathParameters`. Paths
already group by feature, so this is where a feature's routes live
together; lesson 5 puts the feature's service next to them.
-->

---

# Routes are extensions of `Route`

<DrawnAnnotation text="greetRoutes()" label="One call per group of routes, one file per group" :geometry="{ label: { x: 0.55, y: 0.34, width: 0.4 } }" />
<DrawnAnnotation text="fun Route.greetRoutes()" label="An extension of `Route`: the same DSL, from anywhere" :geometry="{ label: { x: 0.78, y: 0.53, width: 0.4 } }" />
<DrawnAnnotation text="delete(&quot;bye&quot;)" label="More than one route per function" :geometry="{ label: { x: 0.7, y: 0.72, width: 0.36 } }" />
<DrawnAnnotation text="helloRoutes()" label="Groups nest: `/greet/{name}/hello/{hour}` lives in another file" :geometry="{ label: { x: 0.7, y: 0.93, width: 0.44 } }" />

<TypeHint :line="2" receiver="Routing">
<TypeHint :line="7" receiver="Route">
<TypeHint :line="8" receiver="RoutingContext">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Route.helloRoutes() {
  get("hello/{hour}") {
    val name: String by call.pathParameters
    val hour: Int by call.pathParameters
    call.respondText("Hello, $name, it's $hour o'clock")
  }
}

// Example
fun Application.module() {
  routing {
    greetRoutes()
  }
}

fun Route.greetRoutes() = route("/greet/{name}") {
  get("bye") {
    val name: String by call.pathParameters
    call.respondText("Bye, $name")
  }
  delete("bye") {
    val name: String by call.pathParameters
    call.respondText("I don't like you either, $name")
  }
  helloRoutes()
}
```

</TypeHint>
</TypeHint>
</TypeHint>

<!--
One big `routing` block does not scale. `routing { }` and `route { }` pass
a `Route` receiver to their block, so any `Route.() -> Unit` function slots
in, and the paths inside stay relative to where it is called; group them by
feature and keep `module()` as the table of contents.
-->

---

# Anything that responds suspends

<InlineCompilerError :line="9" text="respondText" message="Suspend function 'suspend fun ApplicationCall.respondText(text: String, contentType: ContentType? = ..., status: HttpStatusCode? = ..., configure: OutgoingContent.() -> Unit = ...): Unit'\ncan only be called from a coroutine or another suspend function." style="--inline-compiler-error-message-size: 1.05rem">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.module() {
  routing {
    get("/greet/{name}/bye") { bye() }
  }
}

fun RoutingContext.bye() {
  val name: String by call.pathParameters
  call.respondText("Bye, $name")
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

<DrawnAnnotation text="RoutingContext" label="Where `call` comes from: the receiver of every handler" :geometry="{ label: { x: 0.76, y: 0.5, width: 0.4 } }" />
<DrawnAnnotation text="suspend" label="Handlers suspend, and so does anything that responds" :geometry="{ label: { x: 0.76, y: 0.6, width: 0.4 } }" />

<TypeHint :line="2" receiver="Routing">
<TypeHint :line="3" receiver="RoutingContext">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.module() {
  routing {
    get("/greet/{name}/bye") { bye() }
    get("/greet/{name}/hello/{hour}") { hello() }
  }
}

suspend fun RoutingContext.bye() {
  val name: String by call.pathParameters
  call.respondText("Bye, $name")
}

suspend fun RoutingContext.hello() {
  val name: String by call.pathParameters
  val hour: Int by call.pathParameters
  call.respondText("Hello, $name, it's $hour o'clock")
}
```

</TypeHint>
</TypeHint>

<!--
The other way to split: keep the routes together and move the code inside
each of them out. The handler lambda runs with a `RoutingContext` receiver,
which carries `call`; an extension of it can do everything the lambda can,
delegates included. Forget `suspend` and `respondText` does not compile.
-->

---
magic-move
---

# The logic does not need Ktor at all

<DrawnAnnotation text="fun bye(name: String): String" label="No `call`, no `suspend`: a plain function, trivial to test" :geometry="{ label: { x: 0.78, y: 0.76, width: 0.38 } }" />
<DrawnAnnotation text="call.respondText(bye(name))" label="Extract and respond in the route, the logic in between is Kotlin" :geometry="{ label: { x: 0.76, y: 0.34, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.module() {
  routing {
    get("/greet/{name}/bye") {
      val name: String by call.pathParameters
      call.respondText(bye(name))
    }
    get("/greet/{name}/hello/{hour}") {
      val name: String by call.pathParameters
      val hour: Int by call.pathParameters
      call.respondText(hello(name, hour))
    }
  }
}

fun bye(name: String): String = "Bye, $name"

fun hello(name: String, hour: Int): String = "Hello, $name, it's $hour o'clock"
```

<!--
Every handler has three phases: extract the parameters, process, respond.
The first and the last are Ktor, the middle is yours, and the dependency on
the framework shrinks to two lines per route. Lesson 7 tests the routes
through the test host; functions like these are tested with nothing at all.
-->

---

# HTML is just a string, until it is not

> Template engines exist: Mustache, JTE, FreeMarker, Thymeleaf. Or write HTML as code.

<DrawnAnnotation text="$name" label="`name` = `<script>…</script>`: injection" color="red" :geometry="{ label: { x: 0.83, y: 0.41, width: 0.32 } }" />
<DrawnAnnotation text="ContentType.Text.Html" label="Right header, wrong tool" :geometry="{ label: { x: 0.72, y: 0.53, width: 0.3 } }" />

```kotlin
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.hello(name: String) {
  val response = "<h1>Hello, $name!</h1>"
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

suspend fun RoutingContext.hello(name: String) {
  call.respondHtml {
    head {
      title { +"World Greeting Service" }
    }
    body {
      h1 { +"Hello, $name!" }
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

suspend fun RoutingContext.hello(name: String) {
  call.respondHtml {
    head {
      title { +"World Greeting Service" }
    }
    body {
      h1 { +"Hello, $name!" }
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

<DrawnAnnotation text="+&quot;Hello, $name!&quot;" label="`+` appends text, escaped: `<` becomes `&amp;lt;`" :geometry="{ label: { x: 0.72, y: 0.58, width: 0.4 } }" />

```kotlin
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*

suspend fun RoutingContext.hello(name: String) {
  call.respondHtml {
    head {
      title { +"World Greeting Service" }
    }
    body {
      h1 { +"Hello, $name!" }
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

<DrawnAnnotation text="if (name.isEmpty())" label="A conditional, not a template directive" :geometry="{ label: { x: 0.7, y: 0.39, width: 0.36 } }" />
<DrawnAnnotation text="forEach" label="Iteration: `G - I - V - E - N`" :geometry="{ label: { x: 0.55, y: 0.63, width: 0.3 } }" />

```kotlin
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*

suspend fun RoutingContext.spell(name: String) {
  call.respondHtml {
    body {
      h1 {
        if (name.isEmpty()) {
          +"No name"
        } else {
          +"${name.first().uppercaseChar()}"
          name.drop(1).forEach { +" - ${it.uppercaseChar()}" }
        }
      }
    }
  }
}
```

<!--
`GET /spell/{name}` calls this with the captured name. Conditionals and
loops are the language's own: no `{{#if}}`, no `<c:forEach>`, and the IDE
understands all of it.
-->

---

# Templates are functions

<DrawnAnnotation text="notEmpty(name, emptyMessage = &quot;No name&quot;)" label="The check moved out; only the happy path stays" :geometry="{ label: { x: 0.7, y: 0.34, width: 0.36 } }" />

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
suspend fun RoutingContext.spell(name: String) {
  call.respondHtml {
    body {
      h1 {
        notEmpty(name, emptyMessage = "No name") {
          +"${name.first().uppercaseChar()}"
          name.drop(1).forEach { +" - ${it.uppercaseChar()}" }
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

suspend fun RoutingContext.spell(name: String) {
  call.respondHtml {
    body {
      h1 {
        notEmpty(name, emptyMessage = "No name") {
          +"${name.first().uppercaseChar()}"
          name.drop(1).forEach { +" - ${it.uppercaseChar()}" }
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

| `Application.() -> Unit` | `embeddedServer { }`, a module                    |
|--------------------------|---------------------------------------------------|
| `Route.() -> Unit`       | `routing { }`, `route("…") { }`, `Route.byeRoutes()` |
| `HTML.() -> Unit`        | `respondHtml { }`                                 |
| `FlowContent.() -> Unit` | `body { }`, `h1 { }`, `notEmpty { }`             |

<!--
The same shape everywhere in Ktor and in kotlinx.html. A `StringBuilder`
extension that calls `append` a few times is the same pattern one size
smaller: the receiver collects, the block describes. Lesson 1's modules,
this lesson's routes and pages, are all instances of it.
-->

---

# Typed all the way in

- `val name: String by call.pathParameters` → the property name is the key
- `Int`, `enum`, `List<T>`, `T?` → converted and checked; a bad value is a `400`
- `ReadOnlyProperty<Any?, T> { _, prop -> … }` → your own types, the same `by`
- `route("…") { }`, `Route.() -> Unit`, `RoutingContext.() -> Unit` → routes and handlers in their own files
- `respondHtml { body { h1 { +"…" } } }` → HTML as code, escaped

> **Delegates for the parameters, `ContentNegotiation` for the body.**
>
> `kotlinx.html` for the page.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Type the greeting parameters

- Replace every `call.parameters["…"]` with `by call.pathParameters` and `by call.queryParameters`: an `Int` hour, an optional `lang`
- Group the routes under `route("/greet/{name}")`
- Write an `Hour` value class and a `call.hour()` delegate that refuses anything outside `0..23`
- Render the greeting as HTML with `respondHtml`, and try a name with `<b>` in it
- Move each handler into a `suspend fun RoutingContext.…` in its own file
