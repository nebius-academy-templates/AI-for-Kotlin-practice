package pages

internal fun variantId(
    stable: String,
    redesign: String,
): String =
    when (val variant = System.getProperty("ui.variant", "stable")) {
        "stable" -> stable
        "redesign" -> redesign
        else -> error("Unsupported ui.variant '$variant'; expected stable or redesign")
    }
