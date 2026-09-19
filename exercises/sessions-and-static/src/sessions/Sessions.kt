package sessions

import io.ktor.server.application.Application

/** The key the cookie is signed with. A real one comes from configuration. */
val signKey: ByteArray = "6819b57a326945c1968f45236589".hexToByteArray()

/**
 * Exercise 1 / 4: a signed session cookie.
 *
 * `sessions` installs `Sessions` with a `cookie<Visits>("visits")` whose
 * `cookie.path` is `/` and that is signed with
 * `SessionTransportTransformerMessageAuthentication(signKey)`: a client
 * that edits the count gets a fresh session instead.
 */
fun Application.sessions(): Unit = TODO()
