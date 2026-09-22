# Configure the greeting service from a file

The code steps are the exercises in `resources/application.yaml` (in a Gradle
project the file is `src/main/resources/application.yaml`) and
`src/configuration/Application.kt`: the file, the `@Serializable` classes that
mirror it, and `loadConfig()`. Check them with
`./kotlin test --include-module configuration` from `exercises/`.

The last bullet of the slide is not a test:

1. Add a `github` section to the file with `token: "$GITHUB_TOKEN"` and no
   fallback, and a `GitHub` data class with a `token` to `Config`.
2. Run `main` without the variable set. The server does not start: the
   exception names the key that could not be resolved, before a port is
   bound.
3. Run it again with `GITHUB_TOKEN=anything` in the environment, and with
   `PORT=9090`. The log says `Responding at http://0.0.0.0:9090` because
   `"$PORT:8080"` prefers the variable over the fallback.
