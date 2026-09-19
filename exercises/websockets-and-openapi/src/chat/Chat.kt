package chat

import io.ktor.server.application.Application
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

/** One frame in, one frame out: the client sends the text, the echo adds the time. */
@Serializable
data class ChatMessage(val text: String, val timestamp: Long? = null)

/**
 * Exercise 1 / 4: JSON frames need a converter.
 *
 * `webSockets` installs `WebSockets` with
 * `contentConverter = KotlinxWebsocketSerializationConverter(Json)`, so that
 * a handler can `receiveDeserialized` and `sendSerialized` typed frames.
 */
fun Application.webSockets(): Unit = TODO()

/**
 * Exercise 1 / 4: echo with a timestamp.
 *
 * `chat` registers `webSocket("/chat")`. The handler loops: it receives a
 * [ChatMessage] and sends it back with `timestamp = System.currentTimeMillis()`.
 * The loop ends with a `ClosedReceiveChannelException` when the client
 * closes the socket; catch it and let the handler return.
 */
fun Route.chat(): Unit = TODO()
