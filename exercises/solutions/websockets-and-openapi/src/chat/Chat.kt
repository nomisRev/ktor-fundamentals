package chat

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChatMessage(val text: String, val timestamp: Long? = null)

fun Application.webSockets() {
  install(WebSockets) {
    contentConverter = KotlinxWebsocketSerializationConverter(Json)
  }
}

fun Route.chat() {
  webSocket("/chat") {
    try {
      while (true) {
        val message = receiveDeserialized<ChatMessage>()
        sendSerialized(message.copy(timestamp = System.currentTimeMillis()))
      }
    } catch (e: ClosedReceiveChannelException) {
      // the client closed the socket
    }
  }
}
