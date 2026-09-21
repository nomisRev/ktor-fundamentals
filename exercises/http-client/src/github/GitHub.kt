package github

import kotlinx.serialization.Serializable

// The public JSON API of this module is GitHub, like on the slides:
// `GET /users/{username}` is the profile, `GET /users/{username}/repos` the
// repositories. The DTOs carry only the fields the profile page shows;
// `ignoreUnknownKeys` in the client drops the rest.

@Serializable
data class User(
  val name: String?,
  val bio: String?,
  val avatar_url: String?,
)

@Serializable
data class Repo(
  val name: String,
  val description: String?,
  val fork: Boolean,
  val stargazers_count: Int,
)

/** The page `/profile/{name}` answers with: user info plus repositories. */
@Serializable
data class Profile(val user: User, val repos: List<Repo>)
