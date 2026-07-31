package com.sandbox.qa.fakeapi

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** In-memory product state. Resetting the sandbox restores the seed history. */
class RideStore {
    private val nextRideId = AtomicInteger(100)
    private val nextOrderId = AtomicInteger(4)
    private val activeRides = ConcurrentHashMap<Int, ActiveRide>()
    private var activeRideId: Int? = null

    @Volatile
    private var orders: List<Order> = SeedData.orders

    fun orders(): List<Order> = orders

    @Synchronized
    fun active(): ActiveRide? = activeRideId?.let(activeRides::get)

    @Synchronized
    fun create(
        from: String,
        to: String,
        option: RideOption,
    ): ActiveRide? {
        if (activeRideId != null) {
            return null
        }
        val ride =
            ActiveRide(
                id = nextRideId.getAndIncrement(),
                status = DRIVER_FOUND,
                from = from,
                to = to,
                option = option,
                driver = SeedData.driver,
            )
        activeRides[ride.id] = ride
        activeRideId = ride.id
        return ride
    }

    @Synchronized
    fun complete(id: Int): RideCompletionResponse? {
        val current = activeRides[id]?.takeIf { activeRideId == id && it.status == DRIVER_FOUND } ?: return null
        val completed = current.copy(status = COMPLETED)
        activeRides[id] = completed
        activeRideId = null
        val order =
            Order(
                id = nextOrderId.getAndIncrement(),
                date = "Just now",
                from = current.from,
                to = current.to,
                price = current.option.price,
            )
        orders = listOf(order) + orders
        return RideCompletionResponse(completed, order)
    }

    @Synchronized
    fun cancel(id: Int): ActiveRide? {
        val current = activeRides[id]?.takeIf { activeRideId == id && it.status == DRIVER_FOUND } ?: return null
        val cancelled = current.copy(status = CANCELLED)
        activeRides[id] = cancelled
        activeRideId = null
        return cancelled
    }

    @Synchronized
    fun reset() {
        nextRideId.set(100)
        nextOrderId.set(4)
        activeRides.clear()
        activeRideId = null
        orders = SeedData.orders
    }

    companion object {
        const val DRIVER_FOUND = "driver_found"
        const val COMPLETED = "completed"
        const val CANCELLED = "cancelled"
        const val ACTIVE_RIDE_EXISTS_CODE = "ACTIVE_RIDE_EXISTS"
    }
}
