package model

import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    val status: String,
)
