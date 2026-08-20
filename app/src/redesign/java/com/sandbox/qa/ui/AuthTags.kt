package com.sandbox.qa.ui

/**
 * Redesigned auth screens: the dev team renamed every test tag
 * (new naming convention "auth_*"). Tests written against the stable
 * tags fail on this flavor; that is the point of the exercise.
 */
object AuthTags {
    const val PHONE_TITLE = "auth_phone_screen_title"
    const val PHONE_SUBTITLE = "auth_phone_screen_subtitle"
    const val PHONE_COUNTRY_CODE = "auth_country_selector"
    const val PHONE_REGION_OPTION_PREFIX = "auth_country_option_"
    const val PHONE_TEST_NUMBER = "auth_test_number_link"
    const val PHONE_INPUT = "auth_phone_number_field"
    const val PHONE_CONTINUE = "auth_phone_submit"
    const val PHONE_ERROR = "auth_phone_validation_message"

    const val OTP_TITLE = "auth_otp_screen_title"
    const val OTP_HINT = "auth_otp_hint"
    const val OTP_INPUT = "auth_otp_code_field"
    const val OTP_CONFIRM = "auth_otp_submit"
    const val OTP_ERROR = "auth_otp_validation_message"
    const val OTP_RESEND = "auth_otp_resend"
}
