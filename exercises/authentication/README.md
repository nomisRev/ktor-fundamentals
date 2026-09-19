# Protect the greetings three ways

Three modules for three transports, one validation function:
`src/auth/Basic.kt` (`checkCredentials` and `basicModule`),
`src/auth/Session.kt` (`login` and `sessionModule`) and `src/auth/Jwt.kt`
(`jwtModule(secret)` and the `jwtModule()` that reads the secret from
`environment.config`). Check them with
`./kotlin test --include-module authentication` from `exercises/`.

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
