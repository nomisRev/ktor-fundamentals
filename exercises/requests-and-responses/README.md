# Receive a greeting, respond with JSON

The exercises are in `src/requests/Greeting.kt` (the DTOs, whose declaration
is the lesson: the tests do not compile until they exist) and
`src/requests/Application.kt` (the route). Check them with
`./kotlin test --include-module requests-and-responses` from `exercises/`.

The steps that are not code:

1. Run the module from the project you generated in lesson 1 and send the
   request below from IntelliJ IDEA's HTTP client (a `greet.http` file), then
   look at the JSON that comes back.

   ```http
   POST http://localhost:8080/greet
   Content-Type: application/json

   {"type": "hello", "name": "Ada", "tz": "CET"}
   ```

2. Send it again with `Accept: application/xml` and see what happens without
   `xml()` in `ContentNegotiation`: the server has no converter for that
   type and answers `406 Not Acceptable`. The last test pins that answer.
