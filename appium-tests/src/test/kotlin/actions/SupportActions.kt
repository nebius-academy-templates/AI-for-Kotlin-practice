package actions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import pages.SupportPage

/** Interactions and checks for the compact Support FAQ and contact dialog. */
object SupportActions {
    fun awaitReady() {
        SupportPage.title.waitFor()
        SupportPage.fourthFaq.waitFor()
        assertFalse(SupportPage.fifthFaq.isPresent(), "Support should contain exactly four FAQ rows")
    }

    fun expandFirstFaq(expectedAnswer: String) {
        SupportPage.firstFaq.click()
        assertEquals(expectedAnswer, SupportPage.firstFaqAnswer.text, "expanded FAQ answer")
    }

    fun openContactDialog() {
        SupportPage.contactItem.click()
        SupportPage.contactDialog.waitFor()
    }

    fun startChat(expectedStatus: String) {
        SupportPage.chatButton.click()
        assertEquals(expectedStatus, SupportPage.contactStatus.text, "chat acknowledgement")
    }

    fun prepareEmail(expectedStatus: String) {
        SupportPage.emailButton.click()
        assertEquals(expectedStatus, SupportPage.contactStatus.text, "email acknowledgement")
    }

    fun closeContactDialog() {
        SupportPage.closeButton.click()
        SupportPage.contactDialog.waitForGone()
    }
}
