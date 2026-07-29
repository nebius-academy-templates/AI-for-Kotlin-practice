package com.sandbox.qa.fakeapi

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** In-memory product state. Resetting the sandbox restores the seed history. */
class RideStore {
    private val nextRideId = AtomicInteger(100)
    private val nextOrderId = AtomicInteger(4)
    private val activeRides = ConcurrentHashMap<Int, ActiveRide>()

    @Volatile
    private var orders: List<Order> = SeedData.orders

    fun orders(): List<Order> = orders

    @Synchronized
    fun create(
        from: String,
        to: String,
        option: RideOption,
    ): ActiveRide {
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
        return ride
    }

    @Synchronized
    fun complete(id: Int): RideCompletionResponse? {
        val current = activeRides[id]?.takeIf { it.status == DRIVER_FOUND } ?: return null
        val completed = current.copy(status = COMPLETED)
        activeRides[id] = completed
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
        val current = activeRides[id]?.takeIf { it.status == DRIVER_FOUND } ?: return null
        val cancelled = current.copy(status = CANCELLED)
        activeRides[id] = cancelled
        return cancelled
    }

    @Synchronized
    fun reset() {
        nextRideId.set(100)
        nextOrderId.set(4)
        activeRides.clear()
        orders = SeedData.orders
    }

    companion object {
        const val DRIVER_FOUND = "driver_found"
        const val COMPLETED = "completed"
        const val CANCELLED = "cancelled"
    }
}
