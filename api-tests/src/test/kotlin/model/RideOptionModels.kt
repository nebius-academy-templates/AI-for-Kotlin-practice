package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RideOptionsResponse(
    val options: List<RideOption>,
)

@Serializable
data class RideOption(
    val id: Int,
    val name: String,
    val seats: Int,
    @SerialName("price")
    val priceCents: Int,
    val category: String,
    val available: Boolean,
)

@Serializable
data class Driver(
    val name: String,
    val car: String,
    val plate: String,
    val etaMinutes: Int,
)

@Serializable
data class ActiveRide(
    val id: Int,
    val status: String,
    val from: String,
    val to: String,
    val option: RideOption,
    val driver: Driver,
)

@Serializable
data class RideCompletionResponse(
    val ride: ActiveRide,
    val order: Order,
)
