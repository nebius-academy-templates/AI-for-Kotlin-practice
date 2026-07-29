package pages

object NotificationsPage {
    val title = Element("notifications_title")
    val backButton = Element("notifications_back_button")
    val list = Element("notifications_list")
    val markAllRead = Element("notifications_mark_all_read")

    fun item(id: Long) = Element("notification_item_$id")

    fun title(id: Long) = Element("notification_title_$id")

    fun message(id: Long) = Element("notification_message_$id")

    fun newLabel(id: Long) = Element("notification_new_$id")
}
