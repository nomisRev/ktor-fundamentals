# Protect the greetings four ways

Four files for four schemes, one validation function:
`src/auth/Basic.kt` (`checkCredentials`, `basicAuth` and `basicModule`),
`src/auth/Session.kt` (`login`, `sessionAuth` and `sessionModule`),
`src/auth/Jwt.kt` (`jwtAuth(secret)`, `jwtModule(secret)`, `adminModule(secret)`
and the `jwtModule()` that reads the secret from `environment.config`) and
`src/auth/Oidc.kt` (`oidcModule`, against an in-memory OpenID Connect
provider). Check them with `./kotlin test --include-module authentication`
from `exercises/`.

The typed authentication API and the `Oidc` plug-in are experimental in
Ktor 3.6.0; `common.module-template.yaml` opts in with
`-opt-in=io.ktor.utils.io.ExperimentalKtorApi`.

To try a module by hand, start it from the project of lesson 1 with
`embeddedServer(Netty, port = 8080) { jwtModule("supersecret") }` and, in an
`.http` file:

```http
POST http://localhost:8080/login
Authorization: Basic ada supersecret

###
GET http://localhost:8080/greet/ada/hello/9
Authorization: Bearer {{token}}
```
