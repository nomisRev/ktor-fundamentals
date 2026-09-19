package requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Greeting(
  val type: Type = Type.HELLO,
  val name: String,
  @SerialName("tz") val timezone: String? = null,
)

@Serializable
enum class Type {
  @SerialName("hello") HELLO,
  @SerialName("bye") BYE,
}

@Serializable
data class GreetingResponse(val message: String)
