---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 9</div>

# Authentication and authorization

## Who is calling, what is allowed

---

# Authentication says who, authorization says what

| Authentication | who is calling: a password, a token, an identity provider |
|----------------|-----------------------------------------------------------|
| Authorization  | what the caller may do: decided per route                 |

<!--
Two words that travel together and mean different things. Authentication
establishes an identity: a username and password most of the time, a
signed token between programs, or an identity provider such as Google or
GitHub that vouches for the user. Authorization decides what that identity
may do. In Ktor the first is a scheme, a value created once, the second is
a block around routes that takes that value; the whole lesson is those
two halves.
-->

---

# The server challenges, the client answers

<DrawnAnnotation text="401 Unauthorized" label="No credentials yet: the challenge names the scheme and the realm" :geometry="{ label: { x: 0.7, y: 0.336, width: 0.4 } }" />
<DrawnAnnotation text="realm=&quot;Access to secrets&quot;" label="The browser opens its login prompt with this text" :geometry="{ label: { x: 0.74, y: 0.46, width: 0.36 } }" />

```http
GET /greet/alex/hello/9 HTTP/1.1
Host: localhost:8080

HTTP/1.1 401 Unauthorized
WWW-Authenticate: Basic realm="Access to secrets"
```

<!--
HTTP Basic is the oldest scheme and the simplest to understand: the first
request carries nothing, the server refuses with `401` and states in
`WWW-Authenticate` which scheme it accepts and for which realm. A browser
turns that into the grey login dialog; `curl -u` and every HTTP client
know the dance too.
-->

---
magic-move
---

# The server challenges, the client answers

<DrawnAnnotation text="Basic YWxleDpzdXBlcnNlY3JldA==" label="`alex:supersecret` in base64: plain text unless the connection is TLS" color="red" :geometry="{ label: { x: 0.72, y: 0.69, width: 0.44 } }" />

```http
GET /greet/alex/hello/9 HTTP/1.1
Host: localhost:8080

HTTP/1.1 401 Unauthorized
WWW-Authenticate: Basic realm="Access to secrets"
```

```http
GET /greet/alex/hello/9 HTTP/1.1
Host: localhost:8080
Authorization: Basic YWxleDpzdXBlcnNlY3JldA==
```

<!--
The client repeats the request with an `Authorization` header: the
scheme, then `name:password` encoded in base64. Base64 is not encryption;
anyone on the path reads the password, so Basic only makes sense over
HTTPS. The browser remembers the credentials for the realm and sends the
header again on every following request, which is why the user is asked
once.
-->

---

# A scheme is a value, not a name

> Experimental in Ktor 3.6.0: `@OptIn(ExperimentalKtorApi::class)`

<DrawnAnnotation text="basic<UserIdPrincipal>" label="`ktor-server-auth`: the principal type is part of the scheme" :geometry="{ label: { x: 0.72, y: 0.36, width: 0.44 } }" />
<DrawnAnnotation text="&quot;auth&quot;" label="Still named: unique in the application, and in the logs" :geometry="{ label: { x: 0.74, y: 0.47, width: 0.4 } }" />
<DrawnAnnotation text="realm = " label="Sent in `WWW-Authenticate`; the browser shows it in the prompt" :geometry="{ label: { x: 0.74, y: 0.58, width: 0.4 } }" />

```kotlin
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.basic

val basicAuth = basic<UserIdPrincipal>("auth") {
  realm = "Access to secrets"
}
```

<!--
Ktor 3.6.0 adds a second authentication API next to the familiar
`install(Authentication) { basic("auth") { } }`: a scheme is a value.
`basic` is HTTP Basic, the type argument is the principal every route
behind this scheme will get, and the name is still there because the
logs and the mixed case, a classic `authenticate("auth")` around a typed
route, refer to it. Both APIs are marked experimental together with the
Kotlin context parameters they use; the opt-in is one compiler flag or an
`@OptIn` per file. Nothing is protected yet: creating a scheme says how
to authenticate, not where.
-->

---
magic-move
---

# A scheme is a value, not a name

> Experimental in Ktor 3.6.0: `@OptIn(ExperimentalKtorApi::class)`

<DrawnAnnotation text="validate { credentials ->" label="Runs on every request, with the decoded `name` and `password`" :geometry="{ label: { x: 0.74, y: 0.43, width: 0.4 } }" />
<DrawnAnnotation text="UserIdPrincipal(credentials.name)" label="The principal the type argument promised: the built-in one carries a name" :geometry="{ label: { x: 0.76, y: 0.54, width: 0.4 } }" />
<DrawnAnnotation text="null" label="Refused: the challenge is sent again" :geometry="{ label: { x: 0.72, y: 0.66, width: 0.3 } }" />

<TypeHint :line="1" receiver="TypedBasicAuthConfig<UserIdPrincipal>">

```kotlin
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.basic

val basicAuth = basic<UserIdPrincipal>("auth") {
  realm = "Access to secrets"
  validate { credentials ->
    if (credentials.password == "supersecret") {
      UserIdPrincipal(credentials.name)
    } else {
      null
    }
  }
}
```

</TypeHint>

<!--
`validate` receives a `UserPasswordCredential` and answers with the
principal, the object that represents the caller for the rest of the
request, or `null` to refuse. Any class can be the principal; the type
argument fixes which one, and `UserIdPrincipal` is the convenient
built-in. Ktor 2 asked principals to implement a `Principal` interface,
Ktor 3 dropped that, and 3.6 moves the type into the scheme so the
routes can rely on it.
-->

---
magic-move
---

# A scheme is a value, not a name

> Experimental in Ktor 3.6.0: `@OptIn(ExperimentalKtorApi::class)`

<DrawnAnnotation text="checkCredentials(it)" label="One function, shared by both schemes" :geometry="{ label: { x: 0.74, y: 0.383, width: 0.36 } }" />
<DrawnAnnotation text="form<UserIdPrincipal>(&quot;auth-form&quot;)" label="The same credentials in a `POST` body: a login page instead of a prompt" :geometry="{ label: { x: 0.72, y: 0.52, width: 0.44 } }" />
<DrawnAnnotation text="usernameField = &quot;username&quot;" label="The field names of the form" :geometry="{ label: { x: 0.74, y: 0.62, width: 0.3 } }" />

```kotlin
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.basic
import io.ktor.server.auth.form

val basicAuth = basic<UserIdPrincipal>("auth") {
  realm = "Access to secrets"
  validate { checkCredentials(it) }
}

val formAuth = form<UserIdPrincipal>("auth-form") {
  usernameField = "username"
  passwordField = "password"
  validate { checkCredentials(it) }
}
```

<!--
Most websites do not want the browser's dialog: they render a login page
and receive the form. `form` reads the two fields from the body, and
hands `validate` the same `UserPasswordCredential`, so the check is
written once. Each scheme decides what a failure looks like: `basic`
sends `401` with the challenge, `form` can redirect to the login page
with `onUnauthorized`, a few slides on.
-->

---

# Validation returns a principal or `null`

> A password, a database, a directory: the same shape

<DrawnAnnotation text="UserPasswordCredential" label="What `basic` and `form` decode: a `name` and a `password`" :geometry="{ label: { x: 0.32, y: 0.56, width: 0.4 } }" />
<DrawnAnnotation text="UserIdPrincipal?" label="The scheme's principal type; `null` refuses" :geometry="{ label: { x: 0.76, y: 0.56, width: 0.36 } }" />

```kotlin
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential

fun checkCredentials(credentials: UserPasswordCredential): UserIdPrincipal? =
  if (credentials.password == "supersecret") UserIdPrincipal(credentials.name)
  else null
```

<!--
Pulled out of the scheme, the validation is an ordinary function, so it
can be tested, and it can do anything: compare against a hashed password
in the database, call an identity service. The scheme does not care as
long as it gets its principal type or `null`. `validate` is suspending,
so the database call is welcome there.
-->

---
magic-move
---

# Validation returns a principal or `null`

> A password, a database, a directory: the same shape

<DrawnAnnotation text="ldapAuthenticate" label="`ktor-server-auth-ldap`: binds to the directory as that user, with that password" :geometry="{ label: { x: 0.34, y: 0.56, width: 0.44 } }" />
<DrawnAnnotation text="&quot;cn=%s,dc=example&quot;" label="The user's distinguished name, `%s` is the login: ask your admin" :geometry="{ label: { x: 0.76, y: 0.56, width: 0.36 } }" />

```kotlin
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.ldap.ldapAuthenticate

fun checkCredentials(credentials: UserPasswordCredential): UserIdPrincipal? =
  ldapAuthenticate(credentials, "ldap://localhost:389", "cn=%s,dc=example")
```

<!--
LDAP, the Lightweight Directory Access Protocol, is how most organisations
hold their accounts: Active Directory speaks it. `ldapAuthenticate` tries
to bind to the server as the user; a successful bind is the proof, and the
function returns a `UserIdPrincipal`, `null` otherwise. There is no typed
LDAP scheme, and none is needed: this call inside a `validate` block is
the whole integration. The DN format is the one thing you cannot guess:
it depends on how the directory is laid out.
-->

---

# `authenticateWith` takes the scheme, not its name

<DrawnAnnotation text="authenticateWith(basicAuth)" label="The value from the top of the file; every route inside is challenged first" :geometry="{ label: { x: 0.72, y: 0.289, width: 0.42 } }" />
<DrawnAnnotation text="get(&quot;/greet/{name}/hello&quot;)" label="Routes outside the block stay public" :geometry="{ label: { x: 0.74, y: 0.42, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticateWith
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.routes() {
  routing {
    authenticateWith(basicAuth) {
      get("/greet/{name}/hello") {
        val name: String by call.pathParameters
        call.respondText("Hello, $name")
      }
    }
  }
}
```

<!--
This is the authorization half: `authenticateWith` wraps a set of routes
and takes the scheme that guards them. No `install(Authentication)`
anywhere: the scheme registers itself the first time a route uses it. It
is a route node like `route` or `get`, so it nests anywhere in the tree,
inside a classic `authenticate("…")` block too, and a typo in a name is
now a compile error instead of a start-up failure.
-->

---
magic-move
---

# The principal is typed and never `null`

<DrawnAnnotation text="val user: UserIdPrincipal = call.principal" label="What `validate` returned, typed by the scheme: no cast, no `?`" :geometry="{ label: { x: 0.8, y: 0.383, width: 0.3 } }" />
<DrawnAnnotation text="user.name" label="The name the login established, not the one in the URL" :geometry="{ label: { x: 0.74, y: 0.52, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticateWith(basicAuth) {
      get("/greet/{name}/hello") {
        val user: UserIdPrincipal = call.principal
        call.respondText("Hello, ${user.name}")
      }
    }
  }
}
```

<!--
Inside the block `call.principal` is a property whose type comes from the
scheme: `basic<UserIdPrincipal>` guards this route, so this is a
`UserIdPrincipal`, and it is not nullable, because the scheme refused
every request that has no principal before the handler ran. The named
API answered `call.principal<UserIdPrincipal>()` with a nullable: the
type might not match, the route might be optional. The type annotation on
`user` is for the slide; inference does the same. The path parameter
says who the client claims to greet; the principal says who the client
proved to be.
-->

---

# The principal is typed and never `null`

<InlineCompilerError text="principal" message="No context argument for 'authCtx: PrincipalContext<P>' found." :line="4">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    get("/greet/{name}/hello") {
      val user: UserIdPrincipal = call.principal
      call.respondText("Hello, ${user.name}")
    }
  }
}
```

</InlineCompilerError>

<!--
Take the block away and the property is gone with it. `call.principal`
is declared with a context parameter, `PrincipalContext<P>`, that only
`authenticateWith` brings into scope, so a handler outside the block
cannot ask for a principal that nobody established. That is the whole
promise of the typed API: the compiler, not a `?.`, keeps the two halves
of the lesson consistent.
-->

---

# Optional means `principalOrNull`

<DrawnAnnotation text="authenticateWithOptional(basicAuth)" label="Anonymous callers pass; wrong credentials still fail" :geometry="{ label: { x: 0.74, y: 0.289, width: 0.4 } }" />
<DrawnAnnotation text="call.principalOrNull" label="The only accessor in this block: the type says a guest is possible" :geometry="{ label: { x: 0.78, y: 0.383, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticateWithOptional
import io.ktor.server.auth.principalOrNull
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue

fun Application.routes() {
  routing {
    authenticateWithOptional(basicAuth) {
      get("/greet/{name}/hello") {
        val name: String by call.pathParameters
        val user: UserIdPrincipal? = call.principalOrNull
        call.respondText("Hello, ${user?.name ?: name}")
      }
    }
  }
}
```

<!--
A page that greets a visitor by name when it knows them and generically
otherwise. `authenticateWithOptional` lets a request without credentials
through, and inside its block the nullable accessor is the only one that
compiles: the decision the handler has to make is spelled out in the
type. A request with wrong credentials is still refused, so optional
never means "any password will do".
-->

---

# Several schemes share a principal type

<DrawnAnnotation text="authenticateWithAnyOf<UserIdPrincipal>(basicAuth, formAuth)" label="Tried in order; the first that succeeds provides the principal" :geometry="{ label: { x: 0.74, y: 0.42, width: 0.4 } }" />
<DrawnAnnotation text="call.principal.name" label="The common type: a supertype when the schemes differ" :geometry="{ label: { x: 0.76, y: 0.54, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticateWithAnyOf
import io.ktor.server.auth.principal
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticateWithAnyOf<UserIdPrincipal>(basicAuth, formAuth) {
      get("/greet/{name}/hello") {
        call.respondText("Hello, ${call.principal.name}")
      }
    }
  }
}
```

<!--
The browser prompt and the login form guard the same page:
`authenticateWithAnyOf` accepts either, and the type argument is what
the handler gets, so the schemes have to agree on it, or on a common
supertype, an `interface AppUser` implemented by both principals. Only
`call.principal` is available in this block; what a single scheme adds,
the session of the next part for instance, is not.
-->

---

# Schemes differ in the transport

| `basic<P>("…")`            | name and password in a header; only over TLS         |
|----------------------------|-------------------------------------------------------|
| `digest<P>("…")`           | the same prompt, the password hashed on the wire      |
| `form<P>("…")`             | name and password in a `POST` body: a login page      |
| `session<S, P>("…")`       | a cookie written at login: the next part              |
| `jwt<P>("…")`              | a signed token in `Authorization: Bearer`             |
| `bearer<P>("…")`           | an opaque token you look up yourself                  |
| `apiKey<P>("…")`           | a key in a header: `ktor-server-auth-api-key`         |
| `oauth2Session<P, S>("…")` | sign in with Google or GitHub: the last part          |

<!--
All of them live in `ktor-server-auth`, except `jwt` in
`ktor-server-auth-jwt` and `apiKey` in `ktor-server-auth-api-key`.
Digest is Basic's safer sibling: the client proves it knows the password
without sending it. OAuth is the odd one out: it redirects the user to
the provider, receives a code back, and exchanges it for a token; the
typed flow installs those routes for you, and the OpenID Connect plug-in
at the end of the lesson configures it from a single URL. Each scheme
ends in a `validate` block, and each is scoped with the same
`authenticateWith`. The named providers, `install(Authentication)` and
`authenticate("name")`, are still there, and the two APIs nest in either
order.
-->

---

# A login has to outlive its request

> HTTP forgets: the login's outcome must reach the next request

| A session | the server remembers; a cookie or a header carries the identifier |
|-----------|-------------------------------------------------------------------|
| A token   | the client carries the signed proof; the server only verifies it  |

<!--
Basic sends the password every time, which is fine for a script and
wrong for a website. After the login route has done its checks, something
has to represent "logged in" on the next request. Two designs: keep the
state on the server and hand out a reference, the sessions of lesson 5;
or sign the state and hand it to the client, JSON Web Tokens, the usual
choice between services and for mobile clients.
-->

---

# A session scheme names what is stored

<DrawnAnnotation text="session<UserInfo, UserInfo>(&quot;auth-session&quot;)" label="Lesson 5's session class, twice: what the cookie carries, what the handler gets" :geometry="{ label: { x: 0.74, y: 0.5, width: 0.4 } }" />
<DrawnAnnotation text="validate { it }" label="The cookie exists and deserializes: the session object is the principal" :geometry="{ label: { x: 0.74, y: 0.6, width: 0.4 } }" />

```kotlin
import io.ktor.server.auth.session
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(val name: String, val timezone: String)

val sessionAuth = session<UserInfo, UserInfo>("auth-session") {
  validate { it }
}
```

<!--
Same shape, another factory. `session` takes two types: the class stored
for the caller, and the principal the routes work with. Here they are
the same, and the simplest validation returns the session: having the
cookie is the proof, because only the login route writes it. No
`install(Sessions)` and no `cookie<UserInfo>` registration here: the
scheme knows its own transport, in a moment.
-->

---
magic-move
---

# A session scheme names what is stored

<DrawnAnnotation text="onUnauthorized = { call.respondRedirect(&quot;/login&quot;) }" label="No cookie, or `null`: a redirect instead of the default `401`" :geometry="{ label: { x: 0.74, y: 0.66, width: 0.4 } }" />

```kotlin
import io.ktor.server.auth.session
import io.ktor.server.response.respondRedirect
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(val name: String, val timezone: String)

val sessionAuth = session<UserInfo, UserInfo>("auth-session") {
  validate { it }
  onUnauthorized = { call.respondRedirect("/login") }
}
```

<!--
A browser user should land on the login page, not on a status code.
`onUnauthorized` is what the scheme does when validation fails; every
scheme has one, with a sensible default, and a route can override it in
its `authenticateWith` call. The `/login` route comes two slides on.
-->

---
magic-move
---

# Stored and used are two types

<DrawnAnnotation text="session<UserInfo, UserIdPrincipal>" label="The cookie stays a `UserInfo`; the routes see a `UserIdPrincipal`" :geometry="{ label: { x: 0.74, y: 0.47, width: 0.4 } }" />
<DrawnAnnotation text="if (info.name in users)" label="A real check: an account deleted since the login is caught here" :geometry="{ label: { x: 0.74, y: 0.6, width: 0.4 } }" />

```kotlin
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.session
import io.ktor.server.response.respondRedirect
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(val name: String, val timezone: String)

val sessionAuth = session<UserInfo, UserIdPrincipal>("auth-session") {
  validate { info ->
    if (info.name in users) UserIdPrincipal(info.name) else null
  }
  onUnauthorized = { call.respondRedirect("/login") }
}
```

<!--
`validate` has the same contract as for `basic`: it receives the session
and returns the principal or `null`. This is where the two type arguments
earn their place: the session is what the login stored, small and
serializable; the principal is what the handlers want, loaded from the
user table on every request, so a deleted account is refused on its next
click. The handlers stay ignorant of the cookie's class, and the same
`UserIdPrincipal` handler serves Basic and sessions alike.
-->

---
magic-move
---

# The transport belongs to the scheme

<DrawnAnnotation text="SessionTransportType.CookieId(SessionStorageMemory())" label="Lesson 5's choice, made here: the data stays on the server, the cookie carries an id" :geometry="{ label: { x: 0.76, y: 0.42, width: 0.4 } }" />
<DrawnAnnotation text="cookie.maxAgeInSeconds = 3600" label="The same cookie builder as `install(Sessions)`" :geometry="{ label: { x: 0.74, y: 0.52, width: 0.36 } }" />

```kotlin
import io.ktor.server.auth.SessionTransportType
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.session
import io.ktor.server.response.respondRedirect
import io.ktor.server.sessions.SessionStorageMemory
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(val name: String, val timezone: String)

val sessionAuth = session<UserInfo, UserIdPrincipal>("auth-session") {
  transport = SessionTransportType.CookieId(SessionStorageMemory()) {
    cookie.maxAgeInSeconds = 3600
  }
  validate { info ->
    if (info.name in users) UserIdPrincipal(info.name) else null
  }
  onUnauthorized = { call.respondRedirect("/login") }
}
```

<!--
This is the default, spelled out: a cookie named after the scheme that
carries a session id, the data in memory on the server. `HeaderId` is
the same with a header, for programs; `Cookie` and `Header` send the
value itself and need the encrypting transformer of lesson 5, because a
session that says `admin` must not be editable by the client. Memory
storage is for one process; `directorySessionStorage` or your own
`SessionStorage` for production, lesson 5 again.
-->

---

# The login route writes the session

<DrawnAnnotation text="install(sessionAuth)" label="Installs `Sessions` for the scheme, with the transport it declared" :geometry="{ label: { x: 0.74, y: 0.36, width: 0.4 } }" />
<DrawnAnnotation text="sessionAuth.setSession(UserInfo(name, tz))" label="After the checks pass: from now on the cookie says who" :geometry="{ label: { x: 0.76, y: 0.6, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.install
import io.ktor.server.auth.setSession
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.module() {
  install(sessionAuth)
  routing {
    post("/login") {
      if (loggedIn) sessionAuth.setSession(UserInfo(name, tz))
    }
  }
  routes()
}
```

<!--
Two things the scheme cannot do alone. Installing it puts the `Sessions`
plug-in in place with a `cookie<UserInfo>("auth-session")` provider, the
registration lesson 5 wrote by hand; an existing `install(Sessions)`
block calls `sessionAuth.applyTransport()` instead. And a login route
writes the session: `setSession` works on any route, and it has to,
because a route behind the scheme would refuse the caller who has no
session yet. The `if` stands for the checks; wrapping this route in
`authenticateWith(basicAuth)` and storing `call.principal.name` is the
tidy version, and the exercise.
-->

---

# The handler gets both

<DrawnAnnotation text="authenticateWith(sessionAuth)" label="Same scope, another scheme" :geometry="{ label: { x: 0.74, y: 0.289, width: 0.36 } }" />
<DrawnAnnotation text="call.principal" label="What `validate` returned: a `UserIdPrincipal`" :geometry="{ label: { x: 0.8, y: 0.383, width: 0.3 } }" />
<DrawnAnnotation text="call.session.timezone" label="What the login stored, typed as well: only in a session scheme's block" :geometry="{ label: { x: 0.74, y: 0.5, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.auth.session
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticateWith(sessionAuth) {
      get("/greet/{name}/hello") {
        val user: UserIdPrincipal = call.principal
        call.respondText("Hello, ${user.name} in ${call.session.timezone}")
      }
    }
  }
}
```

<!--
The handler is the one from the Basic version, and `call.principal` is
the same `UserIdPrincipal`. A session scheme adds a second typed
property: `call.session` is the stored `UserInfo`, readable and
assignable, so a `POST /switch-timezone` writes `call.session =
call.session.copy(timezone = …)` and `updateSession { }` does it in one
step. No `call.sessions.get<UserInfo>()`: `validate` already read the
cookie.
-->

---
magic-move
---

# Signing out clears the session

<DrawnAnnotation text="sessionAuth.clearSession()" label="Removes the stored session: the next request is anonymous again" :geometry="{ label: { x: 0.74, y: 0.62, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.clearSession
import io.ktor.server.auth.principal
import io.ktor.server.auth.session
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticateWith(sessionAuth) {
      get("/greet/{name}/hello") {
        val user: UserIdPrincipal = call.principal
        call.respondText("Hello, ${user.name} in ${call.session.timezone}")
      }
      post("/logout") {
        sessionAuth.clearSession()
        call.respondText("Bye")
      }
    }
  }
}
```

<!--
The counterpart of `setSession`. Inside the protected block
`call.clearSession()` does the same; on the scheme it works anywhere.
Cookies travel on every request, including one another site triggers,
so a scheme that stays in a browser adds `csrfProtection { }` to its
configuration, the CSRF plug-in scoped to these routes.
-->

---

# A token carries its own claims

> `header.payload.signature`: readable by all, forged by none

<DrawnAnnotation text="&quot;username&quot;" label="A claim: something the issuer asserts about the holder" :geometry="{ label: { x: 0.7, y: 0.352, width: 0.4 } }" />
<DrawnAnnotation text="&quot;exp&quot;" label="A registered claim: expiry in seconds since the epoch, checked by the verifier" :geometry="{ label: { x: 0.72, y: 0.5, width: 0.44 } }" />

```json
{
  "username": "alex",
  "exp": 1767225600
}
```

<!--
A JSON Web Token is three base64 parts joined with dots: a header naming
the algorithm, a payload, and a signature over the first two. The payload
is a JSON object of claims, statements the issuer makes: registered ones
such as `exp`, `iss`, `aud`, `sub`, and your own, `username` here. The
payload is not encrypted, only signed: do not put a password in it.
Tokens are the usual choice between services, because any service with
the key can verify one without a shared session store.
-->

---

# The login signs the token

<DrawnAnnotation text="JWT.create()" label="`com.auth0:java-jwt`: a builder for the payload" :geometry="{ label: { x: 0.74, y: 0.383, width: 0.36 } }" />
<DrawnAnnotation text=".sign(" label="HS256: one shared secret both signs and verifies" :geometry="{ label: { x: 0.74, y: 0.48, width: 0.36 } }" />
<DrawnAnnotation text="secret" label="Hard-coded: from configuration, lesson 2" color="red" :geometry="{ label: { x: 0.74, y: 0.58, width: 0.32 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    post("/login") {
      if (loggedIn) {
        val token = JWT.create()
          .sign(Algorithm.HMAC256(secret))
      }
    }
  }
}
```

<!--
The same login route, issuing a token instead of writing a session. Ktor
does not build tokens; `java-jwt` from Auth0 does, and
`ktor-server-auth-jwt` depends on it. `Algorithm.HMAC256` is symmetric:
whoever can verify can also sign, fine within one application, while
RS256 with a key pair lets other services verify without being able to
issue. A secret in the source is in every git clone and every build.
-->

---
magic-move
---

# The login signs the token

<DrawnAnnotation text=".withClaim(&quot;username&quot;, name)" label="The claim: what the checks established, nothing more" :geometry="{ label: { x: 0.74, y: 0.43, width: 0.4 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    post("/login") {
      if (loggedIn) {
        val token = JWT.create()
          .withClaim("username", name)
          .sign(Algorithm.HMAC256(secret))
      }
    }
  }
}
```

<!--
Claims are the payload; `withClaim` takes strings, numbers, booleans,
dates, lists. `withIssuer` and `withAudience` are the registered ones the
verifier can insist on, so a token minted for one service is refused by
another.
-->

---
magic-move
---

# The login signs the token

<DrawnAnnotation text=".withExpiresAt(Date(System.currentTimeMillis() + 60_000))" label="`exp`: one minute; the verifier refuses the token afterwards" :geometry="{ label: { x: 0.76, y: 0.58, width: 0.36 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.Date

fun Application.routes() {
  routing {
    post("/login") {
      if (loggedIn) {
        val token = JWT.create()
          .withClaim("username", name)
          .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
          .sign(Algorithm.HMAC256(secret))
      }
    }
  }
}
```

<!--
A token cannot be revoked, there is no server-side state to delete, so it
has to expire. Short lifetimes plus a refresh token is the usual pattern;
a minute is for the demo. `java-jwt` still speaks `java.util.Date`;
`Instant` converts with `Date.from`.
-->

---
magic-move
---

# The login signs the token

<DrawnAnnotation text="call.respond(mapOf(&quot;token&quot; to token))" label="A JSON body with the token: the client keeps it, and sends it back" :geometry="{ label: { x: 0.76, y: 0.571, width: 0.36 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.Date

fun Application.routes() {
  routing {
    post("/login") {
      if (loggedIn) {
        val token = JWT.create()
          .withClaim("username", name)
          .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
          .sign(Algorithm.HMAC256(secret))
        call.respond(mapOf("token" to token))
      }
    }
  }
}
```

<!--
`ContentNegotiation` from lesson 3 turns the map into `{"token": "eyJ…"}`.
Where the client keeps it is its problem: memory for a program, secure
storage on a phone. The token is a bearer credential, whoever holds it is
the user, so it deserves the same care as a password.
-->

---

# The token travels as a bearer header

> Not a cookie: the client adds the header itself, every time

<DrawnAnnotation text="eyJhbGciOiJIUzI1NiJ9" label="Header: the algorithm" :geometry="{ label: { x: 0.24, y: 0.5, width: 0.26 } }" />
<DrawnAnnotation text="eyJ1c2VybmFtZSI6ImFsZXgifQ" label="Payload: the claims, base64" :geometry="{ label: { x: 0.53, y: 0.5, width: 0.26 } }" />
<DrawnAnnotation text="9idj00e3…" label="Signature: the HMAC over both" :geometry="{ label: { x: 0.81, y: 0.5, width: 0.26 } }" />

```http
GET /greet/alex/hello/9 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6ImFsZXgifQ.9idj00e3…
```

<!--
Same `Authorization` header as Basic, another scheme: `Bearer` means
"whoever bears this". No browser prompt and no cookie jar, the client
code sets the header, `bearerAuth(token)` in the Ktor client, lesson 8.
Decode the middle part and the claims are right there, which is the
point: the server reads them without a lookup, and the signature is what
makes them trustworthy.
-->

---

# Verify the signature, then the claims

<DrawnAnnotation text="jwt<UserIdPrincipal>(&quot;auth-jwt&quot;)" label="`ktor-server-auth-jwt`: reads `Authorization: Bearer`" :geometry="{ label: { x: 0.74, y: 0.36, width: 0.4 } }" />
<DrawnAnnotation text="verifier(" label="Signature and `exp`, with the same secret: a forged or stale token stops here" :geometry="{ label: { x: 0.72, y: 0.58, width: 0.44 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.jwt.jwt

val jwtAuth = jwt<UserIdPrincipal>("auth-jwt") {
  realm = "Access to secrets"
  verifier(JWT.require(Algorithm.HMAC256(secret)).build())
}
```

<!--
The scheme does two things in order. The verifier is `java-jwt` again:
`require` names the algorithm and secret, `withIssuer` and `withAudience`
add the registered claims to insist on, `build` gives a `JWTVerifier`.
A token whose signature does not match, or whose `exp` has passed, never
reaches `validate`. For RS256, `verifier(jwkProvider, issuer)` fetches
the public keys from the issuer; the OpenID Connect plug-in at the end
does that for you.
-->

---
magic-move
---

# Verify the signature, then the claims

<DrawnAnnotation text="getClaim(&quot;username&quot;)" label="Your rules on the verified claims" :geometry="{ label: { x: 0.74, y: 0.54, width: 0.36 } }" />
<DrawnAnnotation text="UserIdPrincipal(user)" label="The claim becomes the principal: the handler never sees the token" :geometry="{ label: { x: 0.74, y: 0.64, width: 0.4 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.jwt.jwt

val jwtAuth = jwt<UserIdPrincipal>("auth-jwt") {
  realm = "Access to secrets"
  verifier(JWT.require(Algorithm.HMAC256(secret)).build())
  validate { credential ->
    val user = credential.payload.getClaim("username").asString()
    if (user.isNullOrEmpty()) null else UserIdPrincipal(user)
  }
}
```

<!--
`validate` is mandatory for `jwt`: the credential holds the verified
payload, and the block decides whether these claims are enough, and what
the routes get. A missing `username` claim reads as `null`. Returning
`JWTPrincipal(credential.payload)` with `jwt<JWTPrincipal>` keeps the
whole payload for handlers that need more claims; a `UserIdPrincipal`
keeps them identical to the Basic and session versions.
-->

---
magic-move
---

# Verify the signature, then the claims

<DrawnAnnotation text="onUnauthorized = { cause ->" label="Without it a bare `401` with `WWW-Authenticate: Bearer`; the cause says why" :geometry="{ label: { x: 0.74, y: 0.64, width: 0.4 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

val jwtAuth = jwt<UserIdPrincipal>("auth-jwt") {
  realm = "Access to secrets"
  verifier(JWT.require(Algorithm.HMAC256(secret)).build())
  validate { credential ->
    val user = credential.payload.getClaim("username").asString()
    if (user.isNullOrEmpty()) null else UserIdPrincipal(user)
  }
  onUnauthorized = { cause ->
    call.respond(HttpStatusCode.Unauthorized, "Token invalid or expired")
  }
}
```

<!--
A program on the other end wants a status and a reason, not a redirect.
The default answers `401` with `WWW-Authenticate: Bearer`; this one adds
a body. The `cause` is an `AuthenticationFailedCause`: no credentials,
invalid credentials, or an error with a message. `StatusPages` from
lesson 8 could render the `401` instead, for a uniform error format.
-->

---

# The handler never parses the token

<DrawnAnnotation text="authenticateWith(jwtAuth)" label="The third scheme in the same block" :geometry="{ label: { x: 0.74, y: 0.32, width: 0.36 } }" />
<DrawnAnnotation text="call.principal.name" label="The claim the login wrote, shaped by `validate`; the signature vouches for it" :geometry="{ label: { x: 0.74, y: 0.5, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticateWith(jwtAuth) {
      get("/greet/{name}/hello") {
        call.respondText("Hello, ${call.principal.name}")
      }
    }
  }
}
```

<!--
Three schemes, one handler: swap `basicAuth` for `sessionAuth` or
`jwtAuth` and the body does not change, because all three promised a
`UserIdPrincipal`. Nothing here touched a database or a session store:
the token was the proof, and `validate` turned its claim into the
principal once, so no handler repeats the parsing.
-->

---

# Roles are resolved after authentication

> Authorization says what: opt in with `withRoles`

<DrawnAnnotation text="enum class Role : AuthenticationRole" label="Any type with a `name`; an enum is the usual one" :geometry="{ label: { x: 0.74, y: 0.42, width: 0.4 } }" />
<DrawnAnnotation text="jwtAuth.withRoles { user ->" label="Runs on every request, after `validate`: a database, a cache, or the principal itself" :geometry="{ label: { x: 0.74, y: 0.56, width: 0.4 } }" />

```kotlin
import io.ktor.server.auth.AuthenticationRole
import io.ktor.server.auth.withRoles

enum class Role : AuthenticationRole { User, Admin }

val roleAuth = jwtAuth.withRoles { user ->
  if (user.name == "Alex") setOf(Role.User, Role.Admin) else setOf(Role.User)
}
```

<!--
Authentication established who; roles say what they may do, and the
typed API keeps them separate on purpose. `withRoles` wraps an existing
scheme, any of the three, in a role-aware one, and the block resolves
the caller's roles from the principal: a lookup in the user table,
a claim in the token, or a rule as here. A route without a role
requirement never runs it.
-->

---
magic-move
---

# A missing role is `403`, not `401`

<DrawnAnnotation text="roles = setOf(Role.Admin)" label="Every role in the set is required; authenticated without it: `403 Forbidden`" :geometry="{ label: { x: 0.74, y: 0.36, width: 0.4 } }" />
<DrawnAnnotation text="call.principal.roles" label="What the block resolved; only inside a role-aware route" :geometry="{ label: { x: 0.76, y: 0.5, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.auth.roles
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticateWith(roleAuth, roles = setOf(Role.Admin)) {
      get("/greet/{name}/hello") {
        val roles: Set<Role> = call.principal.roles
        call.respondText("Hello, ${call.principal.name}: $roles")
      }
    }
  }
}
```

<!--
Two failures, two statuses: `401` when the caller could not be
identified, `403` when they were identified and are not allowed, and
`onForbidden` on the scheme or the route customises the second like
`onUnauthorized` does the first. `roles = null` resolves the roles
without requiring any, for a handler that shows an admin view to some
and a user view to the rest. A plain `authenticateWith(jwtAuth)` block
has no `roles` property at all: it does not compile.
-->

---

# Too many logins is a `429`

> The login route is where passwords get guessed

<DrawnAnnotation text="install(RateLimit)" label="`ktor-server-rate-limit`: a token bucket per key, the caller's address by default" :geometry="{ label: { x: 0.76, y: 0.352, width: 0.44 } }" />
<DrawnAnnotation text="rateLimiter(limit = 5, refillPeriod = 1.minutes)" label="Five attempts a minute; the sixth is `429 Too Many Requests` with `Retry-After`" :geometry="{ label: { x: 0.575, y: 0.52, width: 0.75 } }" />
<DrawnAnnotation text="rateLimit(RateLimitName(&quot;login&quot;))" label="A route node like `authenticateWith`: only the routes inside count" :geometry="{ label: { x: 0.73, y: 0.634, width: 0.46 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlin.time.Duration.Companion.minutes

fun Application.module() {
  install(RateLimit) {
    register(RateLimitName("login")) {
      rateLimiter(limit = 5, refillPeriod = 1.minutes)
    }
  }
  routing {
    rateLimit(RateLimitName("login")) {
      post("/login") { issueToken() }
    }
  }
}
```

<!--
Authorization has a third answer next to `401` and `403`: not now. The
plug-in keeps a bucket of tokens per key, takes one per request, and
refills the bucket over the period; an empty bucket is a `429` with
`Retry-After` and `X-RateLimit-Remaining` on every response before it.
`requestKey { call -> … }` chooses the key, a user name instead of an
address for the routes behind a scheme, Ktor 3.6 lets it read
`call.principal`; `requestWeight` charges some calls more.
`register { }` without a name is the global limit, `rateLimit { }`
without one applies it. Lesson 6's client retry honours `Retry-After`,
so the two halves agree.
-->

---

# One issuer URL configures the provider

> OpenID Connect: OAuth 2.0 plus an ID token, discovered from `/.well-known/openid-configuration`

<DrawnAnnotation text="suspend fun Application.module()" label="Discovery is an HTTP request: the module suspends until the provider answers" :geometry="{ label: { x: 0.72, y: 0.36, width: 0.44 } }" />
<DrawnAnnotation text="install(Oidc)" label="`ktor-server-auth-oidc`: experimental in 3.6.0, JVM only" :geometry="{ label: { x: 0.74, y: 0.47, width: 0.4 } }" />
<DrawnAnnotation text="issuer = " label="Endpoints, signing keys and algorithms come from the discovery document, refreshed every 15 minutes" :geometry="{ label: { x: 0.72, y: 0.62, width: 0.44 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.oidc.Oidc

suspend fun Application.module() {
  val oidc = install(Oidc)
  val google = oidc.identityProvider("google") {
    issuer = "https://accounts.google.com"
  }
}
```

<!--
Sign in with Google, GitHub, Keycloak, Auth0: an identity provider
authenticates the user and hands your application a signed ID token
saying who they are, OAuth 2.0 with an identity layer on top. Doing that
by hand is a `HttpClient`, an authorization URL, a token URL, a JWKS
endpoint and a verifier; the `Oidc` plug-in derives all of it from the
issuer URL. The module is `suspend` because `identityProvider` fetches
the discovery document before it returns; a missing or mismatched issuer
fails the start-up rather than the first request, and key rotation
reaches the application without a restart.
-->

---
magic-move
---

# An API validates the tokens it is handed

<DrawnAnnotation text="bearer { audience = setOf(&quot;greetings-api&quot;) }" label="Who the token was minted for: your API's identifier, never the login's client id" :geometry="{ label: { x: 0.74, y: 0.36, width: 0.4 } }" />
<DrawnAnnotation text="google.jwtBearer" label="A typed scheme: signature against the provider's keys, issuer, audience, expiry" :geometry="{ label: { x: 0.74, y: 0.5, width: 0.4 } }" />
<DrawnAnnotation text="call.principal.claims.subject" label="`OidcToken.Access`: the verified claims, and `userInfo` when the token carries it" :geometry="{ label: { x: 0.74, y: 0.62, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.oidc.Oidc
import io.ktor.server.auth.principal
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

suspend fun Application.module() {
  val oidc = install(Oidc)
  val google = oidc.identityProvider("google") {
    issuer = "https://accounts.google.com"
    bearer { audience = setOf("greetings-api") }
  }
  routing {
    authenticateWith(google.jwtBearer) {
      get("/greet/{name}/hello") {
        call.respondText("Hello, ${call.principal.claims.subject}")
      }
    }
  }
}
```

<!--
A resource server: an API that accepts tokens somebody else issued, no
login page, no session. `bearer` names the audience the API expects,
and `jwtBearer` is the `jwt` scheme of the previous part configured for
you: `Authorization: Bearer`, the provider's public keys, issuer,
audience, `exp`. A token whose audience is your OAuth client id would
let an ID token pass as an access token, hence the separate identifier.
Providers that issue opaque tokens instead get `introspection { }` and
`introspectionBearer`, a call to the provider per request.
-->

---

# A browser signs in at the provider

<DrawnAnnotation text="oauth {" label="The authorization code flow, PKCE and `state` included" :geometry="{ label: { x: 0.74, y: 0.42, width: 0.4 } }" />
<DrawnAnnotation text="System.getenv(&quot;GOOGLE_CLIENT_ID&quot;)" label="Registered with the provider; lesson 2 has the configuration file" :geometry="{ label: { x: 0.76, y: 0.52, width: 0.36 } }" />
<DrawnAnnotation text="onAuthenticated { idToken ->" label="Runs once, after the callback validated the ID token and stored the session" :geometry="{ label: { x: 0.74, y: 0.64, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.oidc.Oidc
import io.ktor.server.response.respondRedirect

suspend fun Application.module() {
  val oidc = install(Oidc)
  val google = oidc.identityProvider("google") {
    issuer = "https://accounts.google.com"
    oauth {
      clientId = System.getenv("GOOGLE_CLIENT_ID")
      clientSecret = System.getenv("GOOGLE_CLIENT_SECRET")
      onAuthenticated { idToken ->
        call.respondRedirect("/greet/${idToken.userInfo.name}/hello/9")
      }
    }
  }
}
```

<!--
The other scenario: the user is a person in a browser. `oauth` turns the
provider into a login: the user is redirected to Google with a `state`,
a `nonce` and a PKCE challenge, signs in there, and comes back with a
code the plug-in exchanges for tokens, validating the ID token against
the discovery document. The client id and secret are what you registered
at the provider, together with the callback URL. `onAuthenticated` is
where a real application records the login and decides where the user
lands; without it the callback answers an empty `200`.
-->

---

# The plug-in owns the login routes

| `GET /oidc/google/login`     | redirects to the provider with `state`, `nonce` and the PKCE challenge     |
|------------------------------|----------------------------------------------------------------------------|
| `GET /oidc/google/callback`  | exchanges the code, validates the ID token, stores the session             |
| `POST /oidc/google/logout`   | with `logout()`: clears the session, ends it at the provider               |
| `POST /oidc/google/refresh`  | with `refresh()`: renews the tokens before they expire                     |

<!--
Nothing to write for these: the plug-in registers them under the
provider's name, and `loginUri`, `redirectUri` and the `path` arguments
of `logout()` and `refresh()` move them. The callback is the one to
register with the provider as an allowed redirect URI. The session
behind them is a `CookieId` in memory by default, `HttpOnly`,
`SameSite=Lax`, `Secure` outside development, CSRF-protected; the
`sessions { }` block swaps the storage for a persistent one and turns on
`tokenRefreshStrategy` for sessions that outlive their ID token.
-->

---

# The provider's session is a scheme

<DrawnAnnotation text="google.session" label="A `session<OidcToken.Id, OidcToken.Id>` the callback wrote; `401` without it" :geometry="{ label: { x: 0.74, y: 0.36, width: 0.4 } }" />
<DrawnAnnotation text="call.principal.userInfo.name" label="The ID token's claims, normalised: `subject`, `name`, `email`, `picture`" :geometry="{ label: { x: 0.74, y: 0.5, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticateWith(google.session) {
      get("/greet/{name}/hello") {
        call.respondText("Hello, ${call.principal.userInfo.name}")
      }
    }
  }
}
```

<!--
The session part of the lesson again, configured by the plug-in: the
callback stored an `OidcToken.Id` under a fresh session id, and
`google.session` is the session scheme that reads it back; `google` is
the provider the module registered. The principal is the ID token:
`userInfo` has the standard claims, `claims` the raw payload,
`accessToken` and `refreshToken` what the provider handed over.
Protected routes answer `401`, they do not redirect; link to the login
route from your page or set `onUnauthorized` on the route.
-->

---
magic-move
---

# `mapPrincipal` makes it your user again

<DrawnAnnotation text="google.session.mapPrincipal { token ->" label="A new scheme with another principal type; `null` refuses the caller" :geometry="{ label: { x: 0.74, y: 0.3, width: 0.4 } }" />
<DrawnAnnotation text="call.principal.name" label="The handler from the Basic slide, unchanged" :geometry="{ label: { x: 0.76, y: 0.56, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.mapPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

val googleAuth = google.session.mapPrincipal { token ->
  UserIdPrincipal(token.userInfo.name ?: token.userInfo.subject)
}

fun Application.routes() {
  routing {
    authenticateWith(googleAuth) {
      get("/greet/{name}/hello") {
        call.respondText("Hello, ${call.principal.name}")
      }
    }
  }
}
```

<!--
`mapPrincipal` works on any scheme: it runs after the scheme produced its
principal and turns it into yours, here the same `UserIdPrincipal` the
rest of the lesson used, in practice the user record loaded from your
database by the token's `subject`. It runs on every request, not once in
the callback, so a user removed from your table is refused on their next
click even though their session at the provider is fine. Two providers,
Google and GitHub, mapped to the same type, meet in
`authenticateWithAnyOf`.
-->

---

# Who is calling, what is allowed

- `basic<P>("…") { validate { } }` → a scheme with a principal type
- `authenticateWith(scheme) { }` → the routes it protects; `call.principal`, never `null`
- `session<S, P>` and `setSession` → a login that outlives its request
- `jwt<P>` and `JWT.create() … .sign(…)` → a token instead of a session
- `withRoles { }` and `roles = setOf(…)` → `403` per route; `rateLimit(…)` → `429` per caller
- `install(Oidc)` and `identityProvider { issuer }` → a provider vouches

> **Authenticate once, authorize per route.**
>
> The handler only sees the principal.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Protect the greetings four ways

- Guard the greeting routes with `basic<UserIdPrincipal>` and `checkCredentials`
- `POST /login` stores a `UserInfo`; guard with `session<UserInfo, UserIdPrincipal>`
- Issue a JWT from `/login` instead and verify it with `jwt<UserIdPrincipal>`
- Require `Role.Admin` with `withRoles`; read the secret from configuration
- Accept an OpenID Connect provider's tokens with `jwtBearer`
