# Ktor Fundamentals

Slidev deck built on [`slidev-theme-kotlin`](https://www.npmjs.com/package/slidev-theme-kotlin).
Nine lessons from a first `embeddedServer` to configuration and deployment, on Ktor 3.6.0.

## Run locally

Requires Node.js 20.12 or newer and a JDK 21.

```bash
npm install
npm run dev
```

## Present with a phone remote

Use the phone-first **presenter** page for the controls. The `/entry/` URL
printed by Slidev is only an index page; open the presenter URL directly.

1. Put the laptop and phone on the same network. A personal hotspot is the most
   reliable option at a venue; guest Wi-Fi commonly prevents devices from
   reaching each other.
2. Start the presentation:

   ```bash
   npm run present
   ```

3. Open the slideshow and presenter page using the **same LAN hostname/IP**—do
   not use `localhost` for one and the LAN IP for the other. For example, leave
   `http://192.168.x.x:3030/` on the projector and open
   `http://192.168.x.x:3030/presenter/1` on the phone. You can also scan the
   QR code printed after pressing `c`, then replace `/entry/` with
   `/presenter/1` in the opened URL.

For a remote co-presenter on another network, use:

```bash
npx slidev --remote --tunnel
```

The tunnel URL is public, so only use it when needed. For a password-protected
session, start Slidev with `--remote=your-password` instead.

## Compile-check Kotlin snippets

`slidev-kotlin-snippets` (shipped by `slidev-theme-kotlin`) generates one
Kotlin file per Kotlin fence in `lessons/*.md` under
`src/main/kotlin/presentation/snippets/`, with the fence's own imports plus
`presentation.support.*`, the hand-written package holding the DTOs,
services and schemes the slides use without defining them. The generated files
are committed and compile against the same Kotlin 2.4 / Ktor 3.6.0 API the
deck advertises.

After changing a Kotlin fence:

```bash
npm run snippets       # regenerates src/main/kotlin/presentation/snippets/
./gradlew build        # compiles the snippets
```

`npm run snippets:check` fails when the generated sources no longer match the
slides. Mark a fence that is not meant to compile (library internals, an
intermediate step) with `no-compile`, and add whatever a new snippet needs
from its surroundings to `src/main/kotlin/presentation/support/`.

## Exercises

`exercises/` is a [Kotlin Toolchain](https://kotlin-toolchain.org) project with
one module per exercise slide, in deck order: `first-server` (lesson 1),
`configuration` (lesson 2), `requests-and-responses` (lesson 3),
`type-safe-routing` (lesson 4), `sessions-and-static` (lesson 5),
`http-client` (lesson 6), `websockets-and-openapi` (lesson 7),
`status-pages-and-testing` (lesson 8) and `authentication` (lesson 9);
the deployment exercise of lesson 10 is not code and lives in
`deployment/README.md`. Every code-shaped
bullet of a slide is an exercise with a JUnit test that starts the module
with `testApplication` and pins the behaviour over HTTP; the KDoc in the
module's source file states the task. The bullets that are not code
(start.ktor.io, IntelliJ IDEA's HTTP client, `./gradlew buildImage`,
Prometheus) are steps in the module's `README.md`.

Where the body is the lesson, the function is there with a `= TODO()` body.
Where writing the declaration is the lesson (a `@Serializable` DTO, the
`GitHubService` interface), the KDoc describes the shape and
the class is not there yet: the module's tests do not compile until it is, so
the first step of such an exercise is to make the tests compile, the second to
make them pass. Modules are kept apart for that reason: an unfinished module
never blocks the tests of another.

```bash
cd exercises
./kotlin test                                 # every module
./kotlin test --include-module first-server   # one module
```

`exercises/solutions/` mirrors the exercise files (and the `configuration`
module's `application.yaml`) with reference solutions.
`exercises/check-solutions.sh` copies the project to a temporary directory,
overlays the solutions and runs the tests, which is how the tests themselves
are checked (the starter project deliberately does not compile):

```bash
exercises/check-solutions.sh
exercises/check-solutions.sh --include-module authentication
```

## Build and export

```bash
npm run build
npm run export
```
