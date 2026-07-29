package com.sandbox.qa.data

data class RideOption(
    val id: Int,
    val name: String,
    val seats: Int,
    val price: Int,
    val category: String,
    val available: Boolean = true,
)

data class Order(
    val id: Int,
    val date: String,
    val from: String,
    val to: String,
    val price: Int,
)

/** Formats the API's integer euro-cents value for display. */
fun Int.formatEuroCents(): String = "${this / 100}.${(this % 100).toString().padStart(2, '0')} €"

data class Driver(
    val name: String,
    val car: String,
    val plate: String,
    val etaMinutes: Int,
)

data class ActiveRide(
    val id: Int,
    val status: String,
    val from: String,
    val to: String,
    val option: RideOption,
    val driver: Driver,
)
