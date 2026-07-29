package pages

/** Element catalog for the classic View screen; its resource ids need the package prefix. */
object DriverSignupPage {
    private fun viewId(id: String) = Element("com.sandbox.qa:id/$id")

    val backButton = viewId("driver_back_button")
    val title = viewId("driver_title")
    val nameInput = viewId("driver_name_input")
    val carInput = viewId("driver_car_input")
    val submitButton = viewId("driver_submit_button")
    val errorText = viewId("driver_error_text")
    val successText = viewId("driver_success_text")
}
