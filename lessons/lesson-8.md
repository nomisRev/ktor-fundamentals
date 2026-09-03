---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Lesson 8</div>

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
may do. In Ktor the first is a plug-in configured once per application,
the second is a block around routes; the whole lesson is those two halves.
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

# Providers are installed by name

<DrawnAnnotation text="install(Authentication)" label="`ktor-server-auth`: who the caller is, decided once for the application" :geometry="{ label: { x: 0.72, y: 0.242, width: 0.44 } }" />
<DrawnAnnotation text="basic(&quot;auth&quot;)" label="A provider and its name: the routes refer to the name" :geometry="{ label: { x: 0.74, y: 0.36, width: 0.4 } }" />
<DrawnAnnotation text="realm = " label="Sent in `WWW-Authenticate`; the browser shows it in the prompt" :geometry="{ label: { x: 0.74, y: 0.47, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.basic

fun Application.module() {
  install(Authentication) {
    basic("auth") {
      realm = "Access to secrets"
    }
  }
  routes()
}
```

<!--
`Authentication` is the general framework: one plug-in, and inside it as
many providers as the application needs, each with a name. `basic` is
HTTP Basic; the other providers come later. Nothing is protected yet:
installing a provider says how to authenticate, not where.
-->

---
magic-move
---

# Providers are installed by name

<DrawnAnnotation text="validate { credentials ->" label="Runs on every request, with the decoded `name` and `password`" :geometry="{ label: { x: 0.74, y: 0.383, width: 0.4 } }" />
<DrawnAnnotation text="UserIdPrincipal(credentials.name)" label="Any object is a principal: the built-in one carries a name" :geometry="{ label: { x: 0.76, y: 0.5, width: 0.36 } }" />
<DrawnAnnotation text="null" label="Refused: the challenge is sent again" :geometry="{ label: { x: 0.72, y: 0.62, width: 0.3 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.basic

fun Application.module() {
  install(Authentication) {
    basic("auth") {
      realm = "Access to secrets"
      validate { credentials ->
        if (credentials.password == "supersecret") {
          UserIdPrincipal(credentials.name)
        } else {
          null
        }
      }
    }
  }
  routes()
}
```

<!--
`validate` receives a `UserPasswordCredential` and answers with the
principal, the object that represents the caller for the rest of the
request, or `null` to refuse. Ktor 2 asked principals to implement a
`Principal` interface; Ktor 3 accepts any class, `UserIdPrincipal` is
just the convenient built-in one.
-->

---
magic-move
---

# Providers are installed by name

<DrawnAnnotation text="checkCredentials(it)" label="One function, shared by both providers" :geometry="{ label: { x: 0.74, y: 0.383, width: 0.36 } }" />
<DrawnAnnotation text="form(&quot;auth-form&quot;)" label="The same credentials in a `POST` body: a login page instead of a prompt" :geometry="{ label: { x: 0.72, y: 0.477, width: 0.44 } }" />
<DrawnAnnotation text="userParamName = &quot;username&quot;" label="The field names of the form" :geometry="{ label: { x: 0.74, y: 0.6, width: 0.3 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.basic
import io.ktor.server.auth.form

fun Application.module() {
  install(Authentication) {
    basic("auth") {
      realm = "Access to secrets"
      validate { checkCredentials(it) }
    }
    form("auth-form") {
      userParamName = "username"
      passwordParamName = "password"
      validate { checkCredentials(it) }
    }
  }
  routes()
}
```

<!--
Most websites do not want the browser's dialog: they render a login page
and receive the form. `form` reads the two fields from the body, and
hands `validate` the same `UserPasswordCredential`, so the check is
written once. The provider that failed decides the challenge: `basic`
sends `401`, `form` can redirect with `challenge("/login")`.
-->

---

# Validation returns a principal or `null`

> A password, a database, a directory: the same shape

<DrawnAnnotation text="UserPasswordCredential" label="What `basic` and `form` decode: a `name` and a `password`" :geometry="{ label: { x: 0.32, y: 0.56, width: 0.4 } }" />
<DrawnAnnotation text="UserIdPrincipal?" label="Any class can be the principal; `null` refuses" :geometry="{ label: { x: 0.76, y: 0.56, width: 0.36 } }" />

```kotlin
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential

fun checkCredentials(credentials: UserPasswordCredential): UserIdPrincipal? =
  if (credentials.password == "supersecret") UserIdPrincipal(credentials.name)
  else null
```

<!--
Pulled out of the provider, the validation is an ordinary function, so it
can be tested, and it can do anything: compare against a hashed password
in the database, call an identity service. The provider does not care as
long as it gets a principal or `null`. `validate` is suspending, so the
database call is welcome there.
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
function returns a `UserIdPrincipal`, `null` otherwise. The DN format is
the one thing you cannot guess: it depends on how the directory is laid
out.
-->

---

# `authenticate` scopes the protected routes

<DrawnAnnotation text="authenticate(&quot;auth&quot;)" label="The provider by name; every route inside is challenged first" :geometry="{ label: { x: 0.72, y: 0.289, width: 0.42 } }" />
<DrawnAnnotation text="get<Greeting.Hello>" label="Routes outside the block stay public" :geometry="{ label: { x: 0.74, y: 0.42, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticate("auth") {
      get<Greeting.Hello> { req ->
        call.respondText("Hello, ${req.parent.name}")
      }
    }
  }
}
```

<!--
This is the authorization half: `authenticate` wraps a set of routes and
names the provider that guards them. It is a route node like `route` or
`get`, so it nests anywhere in the tree, and several names can be listed,
`authenticate("auth", "auth-form")`, to accept either. `optional = true`
lets anonymous callers through with a `null` principal. The
`AuthenticationChecked` hook is where a plug-in would add role checks.
-->

---
magic-move
---

# `authenticate` scopes the protected routes

<DrawnAnnotation text="call.principal<UserIdPrincipal>()" label="What `validate` returned, asked for by type" :geometry="{ label: { x: 0.8, y: 0.383, width: 0.28 } }" />
<DrawnAnnotation text="user?.name" label="The name the login established, not the one in the URL" :geometry="{ label: { x: 0.74, y: 0.52, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticate("auth") {
      get<Greeting.Hello> { req ->
        val user = call.principal<UserIdPrincipal>()
        call.respondText("Hello, ${user?.name}")
      }
    }
  }
}
```

<!--
Inside the block the handler asks for the principal by the type
`validate` returned. It is nullable in the signature: the type may not
match, or the route may be `optional`. The path parameter says who the
client claims to greet; the principal says who the client proved to be.
-->

---

# Providers differ in the transport

| `basic("…")`      | name and password in a header; only over TLS         |
|-------------------|------------------------------------------------------|
| `digest("…")`     | the same prompt, the password hashed on the wire     |
| `form("…")`       | name and password in a `POST` body: a login page     |
| `session<T>("…")` | a cookie written at login: the next part             |
| `jwt("…")`        | a signed token in `Authorization: Bearer`            |
| `bearer("…")`     | an opaque token you look up yourself                 |
| `oauth("…")`      | sign in with Google or GitHub: an identity provider  |

<!--
All of them live in `ktor-server-auth`, except `jwt` in
`ktor-server-auth-jwt`. Digest is Basic's safer sibling: the client
proves it knows the password without sending it. OAuth is the odd one
out: it redirects the user to the provider, receives a code back, and
exchanges it for a token, several steps and a `HttpClient`; the docs have
a complete Google example. Each provider ends in a `validate` block, and
each is scoped with the same `authenticate`.
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
state on the server and hand out a reference, the sessions of lesson 4;
or sign the state and hand it to the client, JSON Web Tokens, the usual
choice between services and for mobile clients.
-->

---

# The login route writes the session

<DrawnAnnotation text="cookie<UserInfo>(&quot;user&quot;, SessionStorageMemory())" label="Lesson 4: the data stays on the server, the cookie carries an id" :geometry="{ label: { x: 0.76, y: 0.35, width: 0.36 } }" />
<DrawnAnnotation text="call.sessions.set(UserInfo(name, tz))" label="After the checks pass: from now on the cookie says who" :geometry="{ label: { x: 0.76, y: 0.7, width: 0.36 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(val name: String, val timezone: String)

fun Application.module() {
  install(Sessions) {
    cookie<UserInfo>("user", SessionStorageMemory())
  }
  routing {
    post("/login") {
      if (loggedIn) call.sessions.set(UserInfo(name, tz))
    }
  }
}
```

<!--
Nothing new: a registered session class and a route that sets it. The
`if` stands for the checks, a `form` provider around this route is the
tidy version. Server-side storage matters here: a session that says
`admin` must not be editable by the client, and memory storage is only
for one process, lesson 4.
-->

---

# The session is the principal

<DrawnAnnotation text="session<UserInfo>(&quot;auth-session&quot;)" label="A provider that reads the registered session class: no header, no body" :geometry="{ label: { x: 0.74, y: 0.571, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(val name: String, val timezone: String)

fun Application.module() {
  install(Sessions) {
    cookie<UserInfo>("user", SessionStorageMemory())
  }
  install(Authentication) {
    session<UserInfo>("auth-session") {
    }
  }
  routes()
}
```

<!--
Same plug-in, another provider. `session<T>` takes the type registered
with `Sessions`, so both installations name the same class; `Sessions`
has to come first.
-->

---
magic-move
---

# The session is the principal

<DrawnAnnotation text="validate { it }" label="The cookie exists and deserializes: the session object is the principal" :geometry="{ label: { x: 0.74, y: 0.618, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(val name: String, val timezone: String)

fun Application.module() {
  install(Sessions) {
    cookie<UserInfo>("user", SessionStorageMemory())
  }
  install(Authentication) {
    session<UserInfo>("auth-session") {
      validate { it }
    }
  }
  routes()
}
```

<!--
The simplest validation: having the session is the proof, because only
the login route writes it. `UserInfo` needs no supertype; in Ktor 2 it
had to implement `Principal`, in Ktor 3 the data class as it is becomes
the principal.
-->

---
magic-move
---

# The session is the principal

<DrawnAnnotation text="challenge { call.respondRedirect(&quot;/login&quot;) }" label="No cookie, or `null`: a redirect instead of the default `401`" :geometry="{ label: { x: 0.74, y: 0.75, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.response.respondRedirect
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(val name: String, val timezone: String)

fun Application.module() {
  install(Sessions) {
    cookie<UserInfo>("user", SessionStorageMemory())
  }
  install(Authentication) {
    session<UserInfo>("auth-session") {
      validate { it }
      challenge { call.respondRedirect("/login") }
    }
  }
  routes()
}
```

<!--
A browser user should land on the login page, not on a status code.
`challenge` is what the provider does when validation fails; every
provider has one, with a sensible default. The `/login` route is the one
from two slides ago.
-->

---
magic-move
---

# The session is the principal

<DrawnAnnotation text="if (info.name in users) info else null" label="Or a real check; or another object entirely" :geometry="{ label: { x: 0.74, y: 0.54, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.response.respondRedirect
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(val name: String, val timezone: String)

fun Application.module() {
  install(Sessions) {
    cookie<UserInfo>("user", SessionStorageMemory())
  }
  install(Authentication) {
    session<UserInfo>("auth-session") {
      validate { info -> if (info.name in users) info else null }
      challenge { call.respondRedirect("/login") }
    }
  }
  routes()
}
```

<!--
`validate` has the same contract as for `basic`: it receives the session
and returns the principal or `null`. Returning the session itself is
usual; a check against the user table catches accounts deleted since the
login, and returning a different object, `UserIdPrincipal(info.name)`,
keeps the handlers ignorant of the session class. Whatever comes back is
what `call.principal<T>()` finds.
-->

---

# The handler asks for the session type

<DrawnAnnotation text="authenticate(&quot;auth-session&quot;)" label="Same scope, another provider name" :geometry="{ label: { x: 0.74, y: 0.289, width: 0.36 } }" />
<DrawnAnnotation text="call.principal<UserInfo>()" label="No `sessions.get`: `validate` already did it" :geometry="{ label: { x: 0.78, y: 0.383, width: 0.32 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticate("auth-session") {
      get<Greeting.Hello> { req ->
        val user = call.principal<UserInfo>()
        call.respondText("Hello, ${user?.name} in ${user?.timezone}")
      }
    }
  }
}
```

<!--
The handler is the one from the Basic version with the type changed:
`validate` returned the session, so `principal<UserInfo>()` is it. A
handler outside `authenticate` would still read the cookie with
`call.sessions.get<UserInfo>()`, but without the challenge.
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
<DrawnAnnotation text="secret" label="Hard-coded: from configuration, lesson 9" color="red" :geometry="{ label: { x: 0.74, y: 0.58, width: 0.32 } }" />

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
`ContentNegotiation` from lesson 2 turns the map into `{"token": "eyJ…"}`.
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
code sets the header, `bearerAuth(token)` in the Ktor client, lesson 7.
Decode the middle part and the claims are right there, which is the
point: the server reads them without a lookup, and the signature is what
makes them trustworthy.
-->

---

# Verify the signature, then the claims

<DrawnAnnotation text="jwt(&quot;auth-jwt&quot;)" label="`ktor-server-auth-jwt`: reads `Authorization: Bearer`" :geometry="{ label: { x: 0.74, y: 0.289, width: 0.4 } }" />
<DrawnAnnotation text="verifier(" label="Signature and `exp`, with the same secret: a forged or stale token stops here" :geometry="{ label: { x: 0.72, y: 0.45, width: 0.44 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt

fun Application.module() {
  install(Authentication) {
    jwt("auth-jwt") {
      realm = "Access to secrets"
      verifier(JWT.require(Algorithm.HMAC256(secret)).build())
    }
  }
  routes()
}
```

<!--
The provider does two things in order. The verifier is `java-jwt` again:
`require` names the algorithm and secret, `withIssuer` and `withAudience`
add the registered claims to insist on, `build` gives a `JWTVerifier`.
A token whose signature does not match, or whose `exp` has passed, never
reaches `validate`. For RS256, `verifier(jwkProvider, issuer)` fetches
the public keys from the issuer.
-->

---
magic-move
---

# Verify the signature, then the claims

<DrawnAnnotation text="getClaim(&quot;username&quot;)" label="Your rules on the claims: `JWTPrincipal` keeps the payload, `null` refuses" :geometry="{ label: { x: 0.72, y: 0.64, width: 0.44 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.module() {
  install(Authentication) {
    jwt("auth-jwt") {
      realm = "Access to secrets"
      verifier(JWT.require(Algorithm.HMAC256(secret)).build())
      validate { credential ->
        val user = credential.payload.getClaim("username").asString()
        if (user != "") JWTPrincipal(credential.payload) else null
      }
    }
  }
  routes()
}
```

<!--
`validate` is mandatory for `jwt`: the credential holds the verified
payload, and the block decides whether these claims are enough. A missing
`username` claim reads as an empty string. `JWTPrincipal` wraps the
payload for the handlers; returning your own class works as well.
-->

---
magic-move
---

# Verify the signature, then the claims

<DrawnAnnotation text="challenge { _, _ ->" label="Without it a bare `401`; the scheme and the realm come as arguments" :geometry="{ label: { x: 0.74, y: 0.595, width: 0.4 } }" />

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

fun Application.module() {
  install(Authentication) {
    jwt("auth-jwt") {
      realm = "Access to secrets"
      verifier(JWT.require(Algorithm.HMAC256(secret)).build())
      validate { credential ->
        val user = credential.payload.getClaim("username").asString()
        if (user != "") JWTPrincipal(credential.payload) else null
      }
      challenge { _, _ ->
        call.respond(HttpStatusCode.Unauthorized, "Token invalid or expired")
      }
    }
  }
  routes()
}
```

<!--
A program on the other end wants a status and a reason, not a redirect.
The default challenge answers `401` with `WWW-Authenticate: Bearer`; this
one adds a body. `StatusPages` from lesson 7 could render the `401`
instead, for a uniform error format.
-->

---

# The handler reads the claim back

<DrawnAnnotation text="call.principal<JWTPrincipal>()" label="The verified payload, and nothing else: no lookup happened" :geometry="{ label: { x: 0.74, y: 0.32, width: 0.4 } }" />
<DrawnAnnotation text="getClaim(&quot;username&quot;)" label="The claim the login wrote; the signature vouches for it" :geometry="{ label: { x: 0.74, y: 0.54, width: 0.4 } }" />

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing

fun Application.routes() {
  routing {
    authenticate("auth-jwt") {
      get<Greeting.Hello> { req ->
        val principal = call.principal<JWTPrincipal>()
        val user = principal?.payload?.getClaim("username")?.asString()
        call.respondText("Hello, $user")
      }
    }
  }
}
```

<!--
Three providers, one handler shape: `authenticate` by name,
`call.principal<T>()` by type. `JWTPrincipal` also exposes `expiresAt`,
`issuer`, `audience`, and `getClaim` for anything else the login put in.
Nothing here touched a database or a session store: the token was the
proof.
-->

---

# Who is calling, what is allowed

- `install(Authentication) { basic, session<T>, jwt }` → providers
- `validate { … }` → a principal, or `null`
- `authenticate("name") { }` → the routes it protects
- `call.principal<T>()` → what `validate` returned
- `JWT.create() … .sign(…)` → a login without a session

> **Authenticate once, authorize per route.**
>
> The handler only sees the principal.

---
layout: intro
class: exercise-slide
kodee: heart
---

<div class="lesson-number">Exercise</div>

# Protect the greetings three ways

- Guard the greeting routes with `basic` and `checkCredentials`
- Add `POST /login` that sets a `UserInfo` session
- Guard with `session<UserInfo>`, redirect to `/login`
- Issue a JWT from `/login` instead and verify it with `jwt`
- Read the secret from configuration, not the source
