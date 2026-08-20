package com.sandbox.qa.fakeapi

import java.util.concurrent.ConcurrentHashMap

data class BackendSession(
    val states: SandboxStates,
    val rides: RideStore,
    val issuedTokens: MutableSet<String> = ConcurrentHashMap.newKeySet(),
)

/**
 * Isolates mutable backend state by client session. Each API test uses a fresh
 * id, each emulator sends its Android id, and clients without a header (for
 * example an interactive Swagger request) use the default session.
 */
class BackendSessions(
    defaultStates: SandboxStates,
    defaultRideStore: RideStore,
) {
    private val defaultSession = BackendSession(defaultStates, defaultRideStore)
    private val sessions = ConcurrentHashMap<String, BackendSession>()

    fun get(sessionId: String?): BackendSession {
        if (sessionId.isNullOrBlank()) {
            return defaultSession
        }
        return sessions.computeIfAbsent(sessionId) {
            BackendSession(SandboxStates(), RideStore())
        }
    }

    companion object {
        const val HEADER = "X-Sandbox-Session"
    }
}
