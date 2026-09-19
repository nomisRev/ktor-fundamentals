# Make the greeting routes type-safe

Every bullet of this slide is code. The resource classes in
`src/routes/Greeting.kt` are exercises 1 and 2 (the tests do not compile
until `Greeting`, `Greeting.Bye` and `Greeting.Hello` exist), the HTML
handler in `src/routes/Hello.kt` is exercise 3, and `src/routes/Bye.kt` with
`src/routes/Application.kt` are exercise 4: one handler per file, the module
only wires them up. Check them with
`./kotlin test --include-module type-safe-routing` from `exercises/`.

To try the `<b>` in the browser, run the module in the project of lesson 1
and open `http://localhost:8080/greet/%3Cb%3Eada%3C%2Fb%3E/hello/9`: the
DSL escapes the name, so the page shows the tags instead of bold text.
