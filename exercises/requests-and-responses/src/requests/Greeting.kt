package requests

/**
 * Exercise 1 / 5: the request DTO.
 *
 * Declare `Greeting`, a `@Serializable` data class with a `type: Type`
 * (default `Type.HELLO`) and a `name: String`, and the `@Serializable` enum
 * `Type` with the entries `HELLO` and `BYE`, serialized as `"hello"` and
 * `"bye"` (`@SerialName`).
 *
 * Exercise 3 / 5: an optional time zone.
 *
 * Add `timezone: String? = null` to `Greeting`, serialized as `"tz"`. A JSON
 * body without `tz` still deserializes.
 */

/**
 * Exercise 2 / 5: the response DTO.
 *
 * Declare `GreetingResponse`, a `@Serializable` data class with one
 * `message: String`.
 */
