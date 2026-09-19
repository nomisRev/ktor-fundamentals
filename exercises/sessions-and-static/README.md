# Remember the visitor, serve the assets

The exercises are in `src/sessions/Sessions.kt` (the signed cookie),
`src/sessions/Handlers.kt` (the count) and `src/sessions/Application.kt`
(the assets and CORS). `resources/static/` holds the logo and the stylesheet
the page links; in a Gradle project the same folder is
`src/main/resources/static`. Check them with
`./kotlin test --include-module sessions-and-static` from `exercises/`.

The step that is not code: after CORS allows only `https://app.example.com`,
open the browser console on any other page and run

```js
fetch("http://localhost:8080/greet/ada/bye").then(r => r.text()).then(console.log)
```

The request reaches the server, but the server answers `403` without an
`Access-Control-Allow-Origin` header, so the browser refuses to hand the
response to the page. The last test pins that answer.
