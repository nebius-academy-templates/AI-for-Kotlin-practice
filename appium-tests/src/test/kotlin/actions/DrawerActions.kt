package actions

import org.junit.jupiter.api.Assertions.assertEquals
import pages.MapPage
import pages.NotificationsPage

object DrawerActions {
    /**
     * Opens the side drawer and taps "Order history".
     * retryClick guards the drawer's open animation so the tap settles even
     * if it lands mid-transition. (Animations should be OFF in run-suite/CI anyway -
     * this is belt-and-suspenders, not a substitute for that.)
     */
    fun openOrders() {
        MapPage.menuButton.click()
        MapPage.drawerOrdersItem.retryClick(message = "could not open Order history from the drawer")
    }

    /** Opens the drawer and follows Help to the Support screen. */
    fun openSupport() {
        MapPage.menuButton.click()
        MapPage.drawerHelpItem.retryClick(message = "could not open Support from the drawer")
    }

    fun openNotifications(expectedUnreadCount: Int) {
        MapPage.menuButton.click()
        assertEquals(
            expectedUnreadCount.toString(),
            MapPage.drawerNotificationsBadge.text,
            "unread notification count",
        )
        MapPage.drawerNotificationsItem.retryClick(
            message = "could not open Notifications from the drawer",
        )
        NotificationsPage.title.waitFor()
    }
}
