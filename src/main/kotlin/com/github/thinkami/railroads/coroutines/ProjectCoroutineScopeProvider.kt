package com.github.thinkami.railroads.coroutines

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import kotlinx.coroutines.*
import java.util.concurrent.CancellationException

data class DispatcherSet(
    val default: CoroutineDispatcher,
    val io: CoroutineDispatcher,
    val edt: CoroutineDispatcher
)

@Service(Level.PROJECT)
class ProjectCoroutineScopeProvider(
    dispatcherSet: DispatcherSet = defaultDispatchers()
) : Disposable {

    private var disposed = false
    private var currentDispatchers: DispatcherSet = dispatcherSet
    private var supervisorJob: Job = SupervisorJob()
    private var currentScope: CoroutineScope = CoroutineScope(supervisorJob + currentDispatchers.default)

    val scope: CoroutineScope
        get() {
            check(!disposed) { "Scope provider disposed" }
            return currentScope
        }

    fun dispatcherSet(): DispatcherSet = currentDispatchers

    fun reset(dispatcherSet: DispatcherSet = currentDispatchers) {
        check(!disposed) { "Scope provider disposed" }
        supervisorJob.cancel(CancellationException("Scope reset"))
        currentDispatchers = dispatcherSet
        supervisorJob = SupervisorJob()
        currentScope = CoroutineScope(supervisorJob + currentDispatchers.default)
    }

    override fun dispose() {
        disposed = true
        supervisorJob.cancel(CancellationException("Project disposed"))
    }

    companion object {
        fun defaultDispatchers(): DispatcherSet = DispatcherSet(
            default = Dispatchers.Default,
            io = Dispatchers.IO,
            edt = Dispatchers.Main
        )
    }
}
