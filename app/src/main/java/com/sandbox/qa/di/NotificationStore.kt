package com.sandbox.qa.di

import android.content.Context
import com.sandbox.qa.data.ActiveRide
import com.sandbox.qa.data.Order
import com.sandbox.qa.data.formatEuroCents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class RideNotification(
    val id: Long,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
)

/** Device-local notification inbox populated by successful ride lifecycle events. */
class NotificationStore(
    context: Context,
) {
    private val preferences =
        context.getSharedPreferences("sandbox_notifications", Context.MODE_PRIVATE)
    private val _notifications = MutableStateFlow(load())
    val notifications: StateFlow<List<RideNotification>> = _notifications.asStateFlow()

    fun recordDriverFound(ride: ActiveRide) {
        add(
            title = "Driver found",
            message =
                "${ride.driver.name} arrives in ${ride.driver.etaMinutes} min · " +
                    ride.driver.car,
        )
    }

    fun recordRideCompleted(order: Order) {
        add(
            title = "Ride completed",
            message = "${order.from} → ${order.to} · ${order.price.formatEuroCents()}",
        )
    }

    fun recordRideCancelled(ride: ActiveRide) {
        add(
            title = "Ride cancelled",
            message = "${ride.from} → ${ride.to}",
        )
    }

    @Synchronized
    fun markRead(id: Long) {
        update(_notifications.value.map { item -> if (item.id == id) item.copy(isRead = true) else item })
    }

    @Synchronized
    fun markAllRead() {
        update(_notifications.value.map { it.copy(isRead = true) })
    }

    @Synchronized
    fun clear() {
        preferences.edit().clear().apply()
        _notifications.value = emptyList()
    }

    @Synchronized
    private fun add(
        title: String,
        message: String,
    ) {
        val id = preferences.getLong(KEY_NEXT_ID, 1L)
        val item = RideNotification(id = id, title = title, message = message)
        preferences.edit().putLong(KEY_NEXT_ID, id + 1).apply()
        update(listOf(item) + _notifications.value)
    }

    private fun update(items: List<RideNotification>) {
        val serialized = JSONArray()
        items.forEach { item ->
            serialized.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("message", item.message)
                    .put("isRead", item.isRead),
            )
        }
        preferences.edit().putString(KEY_ITEMS, serialized.toString()).apply()
        _notifications.value = items
    }

    private fun load(): List<RideNotification> =
        runCatching {
            val serialized = JSONArray(preferences.getString(KEY_ITEMS, "[]"))
            List(serialized.length()) { index ->
                serialized.getJSONObject(index).let { item ->
                    RideNotification(
                        id = item.getLong("id"),
                        title = item.getString("title"),
                        message = item.getString("message"),
                        isRead = item.optBoolean("isRead"),
                    )
                }
            }
        }.getOrDefault(emptyList())

    private companion object {
        const val KEY_ITEMS = "items"
        const val KEY_NEXT_ID = "next_id"
    }
}
