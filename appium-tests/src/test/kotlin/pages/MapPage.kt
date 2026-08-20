package pages

/** Element catalog for the map screen (plus the side-drawer entry it hosts). */
object MapPage {
    val menuButton = Element("map_menu_button")
    val pickupField = Element("map_from_field")
    val destinationField = Element("map_to_field")
    val pullToRefresh = Element("rides_pull_to_refresh")
    val pullHint = Element("rides_pull_hint")
    val removedSearchButton = Element("map_search_button")
    val ridesList = Element("rides_list")

    val ridesLoading = Element("rides_loading")
    val ridesError = Element("rides_error")
    val ridesRetryButton = Element("rides_retry_button")

    val orderRideButton = Element("ride_order_button")
    val rideActionLoading = Element("ride_action_loading")
    val driverSearchingTitle = Element("ride_driver_searching_title")
    val driverFoundTitle = Element("ride_driver_found_title", defaultTimeoutSec = 20)
    val driverRoute = Element("ride_driver_route")
    val driverCard = Element("ride_driver_card")
    val completeRideButton = Element("ride_complete_button")
    val cancelRideButton = Element("ride_cancel_button")
    val completedTitle = Element("ride_completed_title", defaultTimeoutSec = 20)
    val completedPrice = Element("ride_completed_price")
    val bookAnotherButton = Element("ride_book_another_button")
    val backHomeButton = Element("ride_back_home_button")
    val statusMessage = Element("ride_status_message", defaultTimeoutSec = 20)

    // The tag is on a container, so tests assert presence rather than text.
    val regionBanner = Element("region_banner")

    val drawerOrdersItem = Element("drawer_orders_item")
    val drawerNotificationsItem = Element("drawer_notifications_item")
    val drawerNotificationsBadge = Element("drawer_notifications_badge")
    val drawerHelpItem = Element("drawer_help_item")
    val drawerBecomeDriverButton = Element("drawer_become_driver_button")

    fun ridePrice(rideId: Int) = Element("ride_price_$rideId", defaultTimeoutSec = 15)

    fun rideName(rideId: Int) = Element("ride_name_$rideId", defaultTimeoutSec = 15)

    fun rideOption(rideId: Int) = Element("ride_option_$rideId", defaultTimeoutSec = 15)

    fun rideSelected(rideId: Int) = Element("ride_selected_$rideId", defaultTimeoutSec = 15)
}
