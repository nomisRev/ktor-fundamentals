package github

import kotlinx.serialization.Serializable

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

@Serializable
data class Profile(val user: User, val repos: List<Repo>)
