package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SandboxSnapshot(
    @SerialName("slow_backend_response")
    val slowBackendResponse: Boolean,
    @SerialName("backend_error")
    val backendError: Boolean,
    @SerialName("car_unavailable")
    val carUnavailable: Boolean,
    @SerialName("driver_not_found")
    val driverNotFound: Boolean,
    @SerialName("intermittent_backend_delay")
    val intermittentBackendDelay: Boolean,
    @SerialName("region_unavailable")
    val regionUnavailable: Boolean,
)
