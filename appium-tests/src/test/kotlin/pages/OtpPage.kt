package pages

/** Element catalog for the OTP screen. */
object OtpPage {
    val title = Element("otp_title")
    val codeInput = Element("otp_input")
    val confirmButton = Element("otp_continue_button")
    val errorLabel = Element("otp_error")

    /** Disabled while the 30 s cooldown ticks; its label shows the countdown. */
    val resendButton = Element("otp_resend_button")
}
