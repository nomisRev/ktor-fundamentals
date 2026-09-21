# Type the greeting parameters

Every bullet of this slide is code. The delegated parameters in
`src/routes/Hello.kt` and `src/routes/Bye.kt` are exercise 1, the route
group in `src/routes/Application.kt` is exercise 2, the `Hour` value class
and its delegate in `src/routes/Hour.kt` are exercise 3, and the HTML page in
`src/routes/Hello.kt` is exercise 4: one handler per file, the module only
wires them up. Check them with
`./kotlin test --include-module type-safe-routing` from `exercises/`.

To try the `<b>` in the browser, run the module in the project of lesson 1
and open `http://localhost:8080/greet/%3Cb%3Eada%3C%2Fb%3E/hello/9`: the
DSL escapes the name, so the page shows the tags instead of bold text.
`http://localhost:8080/greet/ada/hello/25` answers `400 Bad Request`, from
your own delegate; `.../hello/nine` does too, from Ktor's.
