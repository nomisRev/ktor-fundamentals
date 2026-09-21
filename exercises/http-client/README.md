# Call another API from a route

The public JSON API is GitHub, as on the slides; the DTOs are given in
`src/github/GitHub.kt`. The rest follows the slides: the client
(`Client.kt`, exercise 1), the concurrent handler (`Profile.kt`, exercise 2),
the `AutoCloseable` implementation (`GitHubService.kt`, exercise 3) and the
DI wiring (`Application.kt`, exercise 4). Check them with
`./kotlin test --include-module http-client` from `exercises/`.

The tests never reach the real GitHub: `externalServices { hosts(...) }`
mocks `https://api.github.com` inside `testApplication`, which is why
`module` takes the client as a parameter. To see it against the real API,
add the module to the project of lesson 1 and open
`http://localhost:8080/profile/JetBrains`.
