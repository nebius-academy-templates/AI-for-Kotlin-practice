package com.sandbox.qa.data

open class ApiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class DriverNotFoundException(
    message: String,
) : ApiException(message)

class ActiveRideExistsException(
    message: String,
) : ApiException(message)

/**
 * Load-bearing values shared by local UI validation and the HTTP product
 * contract. Runtime ride data itself comes from [HttpRideRepository].
 */
object SandboxContract {
    const val VALID_OTP = "1234"
    const val INVALID_OTP_ERROR = "Invalid code"
    const val TEST_PHONE_NUMBER = "999999999"
    const val MINIVAN_RIDE_ID = 3
    const val NO_CARS_FOUND_ERROR = "No cars found for this route"
    const val ACTIVE_RIDE_EXISTS_CODE = "ACTIVE_RIDE_EXISTS"
}
