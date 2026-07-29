package model

import kotlinx.serialization.Serializable

@Serializable
data class RegionStatus(
    val available: Boolean,
)
