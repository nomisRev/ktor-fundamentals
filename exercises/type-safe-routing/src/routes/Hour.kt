package routes

import io.ktor.server.application.ApplicationCall
import kotlin.properties.ReadOnlyProperty

/** An hour on the clock: `0..23`, nothing else. */
@JvmInline
value class Hour(val value: Int)

/**
 * Exercise 3 / 4: strong types at the edge.
 *
 * `hour` returns a `ReadOnlyProperty<Any?, Hour>` so that a handler writes
 * `val hour by call.hour()`. The delegate reads
 * `parameters.getOrFail<Int>(prop.name)` (without the type argument the
 * `String` overload wins), refuses anything outside `0..23` with
 * `ParameterConversionException(prop.name, "Hour")`, which Ktor answers with
 * `400 Bad Request`, and wraps the rest in [Hour]. No reflection: the
 * property name comes with the delegate.
 */
fun ApplicationCall.hour(): ReadOnlyProperty<Any?, Hour> = TODO()
