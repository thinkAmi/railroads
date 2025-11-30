package com.github.thinkami.railroads.workflow

import com.github.thinkami.railroads.coroutines.DispatcherSet
import com.github.thinkami.railroads.models.routes.BaseRoute
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class RoutesUiCoordinatorImplLightTest : LightPlatformTestCase() {

    private val dispatchers = DispatcherSet(
        default = Dispatchers.Unconfined,
        io = Dispatchers.Unconfined,
        edt = Dispatchers.Unconfined
    )

    private class FakeUiActions : UiActions {
        var loadingCalled = false
        var routes: List<BaseRoute>? = null
        var errorCalled = false
        var lastThread: String? = null

        override fun renderLoading() {
            loadingCalled = true
            lastThread = Thread.currentThread().name
        }

        override fun renderRoutes(routes: List<BaseRoute>) {
            this.routes = routes
            lastThread = Thread.currentThread().name
        }

        override fun renderError() {
            errorCalled = true
            lastThread = Thread.currentThread().name
        }
    }

    private class RecordingObservability : RoutesObservability {
        val transitions = mutableListOf<RouteLoadState>()
        val threadViolations = mutableListOf<Pair<Boolean, String>>()
        val blockingWarnings = mutableListOf<String>()

        override fun onStateTransition(state: RouteLoadState) {
            transitions.add(state)
        }

        override fun onUiDispatcherViolation(isDispatchThread: Boolean, threadName: String) {
            threadViolations.add(isDispatchThread to threadName)
        }

        override fun onPotentialBlockingOperation(location: String) {
            blockingWarnings.add(location)
        }
    }

    fun testUpdatesLoadingOnRunning() = runBlocking {
        val view = FakeUiActions()
        val coordinator = RoutesUiCoordinatorImpl(dispatchers, { view })

        coordinator.update(RouteLoadState.Running)

        assertTrue(view.loadingCalled)
    }

    fun testUpdatesRoutesOnSuccess() = runBlocking {
        val view = FakeUiActions()
        val coordinator = RoutesUiCoordinatorImpl(dispatchers, { view })
        val route = object : BaseRoute(
            module,
            "GET",
            "/users",
            "users",
            "UsersController",
            "index"
        ) {}

        coordinator.update(RouteLoadState.Success(listOf(route), null))

        assertEquals(listOf(route), view.routes)
    }

    fun testUpdatesErrorOnFailure() = runBlocking {
        val view = FakeUiActions()
        val coordinator = RoutesUiCoordinatorImpl(dispatchers, { view })

        coordinator.update(RouteLoadState.Error("boom"))

        assertTrue(view.errorCalled)
    }

    fun testReportsDispatcherViolationWhenNotOnEdt() = runBlocking {
        val view = FakeUiActions()
        val observability = RecordingObservability()
        val coordinator = RoutesUiCoordinatorImpl(dispatchers, { view }, observability)

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            coordinator.update(RouteLoadState.Running)
        }

        assertTrue(observability.threadViolations.isNotEmpty())
        val (isDispatchThread, _) = observability.threadViolations.last()
        assertFalse(isDispatchThread)
    }

    fun testRunsUiUpdatesOnProvidedEdtDispatcher() = runBlocking {
        val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "EDT-TEST") }
        val edt = executor.asCoroutineDispatcher()
        val customDispatchers = Dispatchers.Unconfined.let {
            DispatcherSet(
                default = it,
                io = it,
                edt = edt
            )
        }
        val view = FakeUiActions()
        val coordinator = RoutesUiCoordinatorImpl(customDispatchers, { view })

        coordinator.update(RouteLoadState.Error("boom"))

        assertTrue(view.errorCalled)
        assertTrue(view.lastThread?.contains("EDT-TEST") == true)
        executor.shutdownNow()
    }
}
