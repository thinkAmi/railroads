package com.github.thinkami.railroads.workflow

import com.intellij.openapi.diagnostic.Logger

interface RoutesObservability {
    fun onStateTransition(state: RouteLoadState)
    fun onUiDispatcherViolation(isDispatchThread: Boolean, threadName: String)
    fun onPotentialBlockingOperation(location: String)
}

class RoutesLoggerObservability(
    private val logger: Logger = Logger.getInstance(RoutesLoggerObservability::class.java)
) : RoutesObservability {
    override fun onStateTransition(state: RouteLoadState) {
        logger.info("Route load state: ${state.javaClass.simpleName}")
    }

    override fun onUiDispatcherViolation(isDispatchThread: Boolean, threadName: String) {
        if (!isDispatchThread) {
            logger.warn("UI update not on EDT (isDispatchThread=$isDispatchThread, thread=$threadName)")
        } else {
            logger.info("UI update on EDT (thread=$threadName)")
        }
    }

    override fun onPotentialBlockingOperation(location: String) {
        logger.warn("Potential blocking operation on EDT: $location")
    }
}
