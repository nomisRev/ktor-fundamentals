package routes

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.ParameterConversionException
import io.ktor.server.util.getOrFail
import kotlin.properties.ReadOnlyProperty

@JvmInline
value class Hour(val value: Int)

fun ApplicationCall.hour() = ReadOnlyProperty<Any?, Hour> { _, prop ->
  val raw = parameters.getOrFail<Int>(prop.name)
  if (raw !in 0..23) throw ParameterConversionException(prop.name, "Hour")
  Hour(raw)
}
