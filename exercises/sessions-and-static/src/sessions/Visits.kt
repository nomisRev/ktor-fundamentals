package sessions

import kotlinx.serialization.Serializable

/** The session: how often this visitor said hello. */
@Serializable
data class Visits(val count: Int)
