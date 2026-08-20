package com.sandbox.qa.data

/**
 * Data layer contract for the ride-hailing sandbox.
 *
 * ViewModels depend on this contract while the runtime implementation talks
 * to the deterministic local HTTP backend.
 */
interface RideRepository {
    suspend fun requestGeolocation(): String

    suspend fun getRideOptions(
        from: String,
        to: String,
    ): List<RideOption>

    suspend fun getOrders(): List<Order>

    /** Returns the unfinished ride for this sandbox session, or null when none exists. */
    suspend fun getActiveRide(): ActiveRide?

    suspend fun createRide(
        from: String,
        to: String,
        rideOptionId: Int,
    ): ActiveRide

    suspend fun completeRide(rideId: Int): Order

    suspend fun cancelRide(rideId: Int): ActiveRide

    suspend fun resetSandbox()
}
