package actions

import org.junit.jupiter.api.Assertions.assertEquals
import pages.MapPage
import pages.NotificationsPage

object NotificationActions {
    fun assertNotification(
        id: Long,
        expectedTitle: String,
        expectedMessage: String,
    ) {
        NotificationsPage.item(id).waitFor()
        assertEquals(expectedTitle, NotificationsPage.title(id).text, "notification $id title")
        assertEquals(expectedMessage, NotificationsPage.message(id).text, "notification $id message")
        NotificationsPage.newLabel(id).waitFor()
    }

    fun markRead(id: Long) {
        NotificationsPage.item(id).click()
        NotificationsPage.newLabel(id).waitForGone()
    }

    fun markAllRead(vararg ids: Long) {
        NotificationsPage.markAllRead.click()
        ids.forEach { NotificationsPage.newLabel(it).waitForGone() }
        NotificationsPage.markAllRead.waitForGone()
    }

    fun returnToRideForm() {
        NotificationsPage.backButton.click()
        MapPage.pickupField.waitFor()
    }
}
