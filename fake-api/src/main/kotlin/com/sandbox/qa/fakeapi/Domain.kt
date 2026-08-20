package com.sandbox.qa.fakeapi

import kotlinx.serialization.Serializable

/**
 * Wire models of the fake ride-hailing API. [RideOption] and [Order] are field
 * for field the same shapes as the mobile app's data layer (Models.kt in
 * app/): one product domain, two test targets. Prices travel as integer euro
 * cents on the wire; currency formatting ("~29.70 EUR" on the map,
 * "29.70 EUR" in history) is a UI concern and never part of this contract.
 */
@Serializable
data class RideOption(
    val id: Int,
    val name: String,
    val seats: Int,
    val price: Int,
    val category: String,
    /** car_unavailable marks the Minivan row unavailable instead of dropping it: the row stays on the wire like it stays on the screen. */
    val available: Boolean = true,
)

@Serializable
data class Order(
    val id: Int,
    val date: String,
    val from: String,
    val to: String,
    val price: Int,
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
data class CreateRideRequest(
    val from: String,
    val to: String,
    val rideOptionId: Int,
)

@Serializable
data class RideCompletionResponse(
    val ride: ActiveRide,
    val order: Order,
)

@Serializable
data class LocationResponse(
    val location: String,
)

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
data class TokenResponse(
    val token: String,
)

@Serializable
data class StatusResponse(
    val status: String,
)

@Serializable
data class ErrorResponse(
    val error: String,
)

@Serializable
data class CodedErrorResponse(
    val error: String,
    val code: String,
)

@Serializable
data class RegionStatus(
    val available: Boolean,
)

@Serializable
data class SandboxStateRequest(
    val condition: String,
    val enabled: Boolean,
)

@Serializable
data class RideOptionsResponse(
    val options: List<RideOption>,
)

@Serializable
data class OrdersResponse(
    val orders: List<Order>,
)

/**
 * Seed data of the fake backend. The values are load bearing and shared with
 * the mobile app and the course fixtures (AGENTS.md, "Load-bearing values"):
 * reference tests assert them, do not change them casually.
 */
object SeedData {
    const val VALID_OTP = "1234"
    const val MIN_PHONE_DIGITS = 8
    const val PHONE_VALIDATION_ERROR = "Phone number must contain at least $MIN_PHONE_DIGITS digits"

    // Tariff disabled by the car_unavailable state, same id as in the app.
    const val MINIVAN_RIDE_ID = 3

    val rideOptions =
        listOf(
            RideOption(1, "Yellow", 4, 2970, "Official taxi"),
            RideOption(2, "Turquoise", 4, 3454, "Official taxi"),
            RideOption(MINIVAN_RIDE_ID, "Minivan", 8, 3905, "Official taxi"),
        )

    val orders =
        listOf(
            Order(1, "June 12, 22:55", "Oak Avenue", "Market Street", 2970),
            Order(2, "June 10, 09:14", "City Center", "Airport Terminal", 1450),
            Order(3, "June 2, 18:30", "Pine Street", "River Road", 980),
        )

    val driver = Driver("Alex Morgan", "Toyota Corolla", "QA 1234", 4)
}
