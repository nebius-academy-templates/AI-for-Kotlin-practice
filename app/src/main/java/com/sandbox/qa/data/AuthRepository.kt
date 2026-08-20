package com.sandbox.qa.data

/** Authentication calls used by the phone and OTP onboarding screens. */
interface AuthRepository {
    suspend fun requestOtp(phone: String)

    suspend fun verifyOtp(
        phone: String,
        code: String,
    )
}
