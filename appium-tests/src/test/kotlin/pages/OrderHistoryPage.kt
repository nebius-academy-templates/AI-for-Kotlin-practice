package pages

/** Element catalog for the order-history screen. */
object OrderHistoryPage {
    val title = Element("orders_title")
    val ordersList = Element("orders_list", defaultTimeoutSec = 15)
    val backButton = Element("orders_back_button")

    val errorLabel = Element("orders_error", defaultTimeoutSec = 15)

    fun orderPrice(orderId: Int) = Element("order_price_$orderId", defaultTimeoutSec = 15)

    fun orderRoute(orderId: Int) = Element("order_route_$orderId", defaultTimeoutSec = 15)
}
