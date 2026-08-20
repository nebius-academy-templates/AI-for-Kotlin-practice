package model

import kotlinx.serialization.Serializable

@Serializable
data class PhoneRequest(
    val phone: String,
)

@Serializable
data class OtpRequest(
    val phone: String,
    val code: String,
)

@Serializable
data class CreateRideRequest(
    val from: String,
    val to: String,
    val rideOptionId: Int,
)

@Serializable
data class SandboxStateRequest(
    val condition: String,
    val enabled: Boolean,
)
