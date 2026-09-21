package auth

import io.ktor.server.auth.AuthenticationRole
import kotlinx.serialization.Serializable

// The protected route of every module is lesson 3's greeting,
// `GET /greet/{name}/hello/{hour}`, as a plain string route.

/** The session class the login writes and the session scheme reads back. */
@Serializable
data class UserInfo(val name: String, val timezone: String)

/** The roles the admin greeting checks; an enum's `name` satisfies `AuthenticationRole`. */
enum class Role : AuthenticationRole { User, Admin }
