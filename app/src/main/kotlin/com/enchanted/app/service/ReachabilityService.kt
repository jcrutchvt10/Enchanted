package com.enchanted.app.service

import com.enchanted.app.data.remote.NimClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReachabilityService @Inject constructor(
    private val nimClient: NimClient
) {
    private val _isReachable = MutableStateFlow(true)
    val isReachable: StateFlow<Boolean> = _isReachable.asStateFlow()

    private var job: Job? = null
    private var pingIntervalMs: Long = 5000

    fun startChecking(intervalMs: Long = 5000) {
        pingIntervalMs = intervalMs
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                _isReachable.value = nimClient.reachable()
                delay(pingIntervalMs)
            }
        }
    }

    fun stopChecking() {
        job?.cancel()
        job = null
    }

    suspend fun checkOnce() {
        _isReachable.value = nimClient.reachable()
    }
}
