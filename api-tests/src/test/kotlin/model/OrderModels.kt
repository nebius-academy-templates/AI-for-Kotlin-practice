package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrdersResponse(
    val orders: List<Order>,
)

@Serializable
data class Order(
    val id: Int,
    val date: String,
    val from: String,
    val to: String,
    @SerialName("price")
    val priceCents: Int,
)
