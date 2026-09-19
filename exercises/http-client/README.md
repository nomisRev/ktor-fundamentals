# Call another API from a route

The public JSON API is GitHub, as on the slides; the DTOs are given in
`src/github/GitHub.kt`, the resources are exercise 1 (the tests do not
compile until `object GitHub` exists). The rest follows the slides: the
client (`Client.kt`), the concurrent handler (`Profile.kt`), the
`AutoCloseable` implementation (`GitHubService.kt`) and the DI wiring
(`Application.kt`). Check them with
`./kotlin test --include-module http-client` from `exercises/`.

The tests never reach the real GitHub: `externalServices { hosts(...) }`
mocks `https://api.github.com` inside `testApplication`, which is why
`module` takes the client as a parameter. To see it against the real API,
add the module to the project of lesson 1 and open
`http://localhost:8080/profile/JetBrains`.
