package com.github.thinkami.railroads.workflow

import com.github.thinkami.railroads.coroutines.DispatcherSet
import com.github.thinkami.railroads.coroutines.ProjectCoroutineScopeProvider
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class RoutesRunnerServiceLightTest : LightPlatformTestCase() {

    private val testDispatchers = DispatcherSet(
        default = Dispatchers.Unconfined,
        io = Dispatchers.Unconfined,
        edt = Dispatchers.Unconfined
    )

    private class RecordingUi : RoutesUiCoordinator {
        val states = mutableListOf<RouteLoadState>()
        override suspend fun update(state: RouteLoadState) {
            states.add(state)
        }
    }

    private class RecordingProgressRunner : ProgressRunner {
        var title: String? = null
        override suspend fun <T> run(project: com.intellij.openapi.project.Project, title: String, action: suspend () -> T): T {
            this.title = title
            return action()
        }
    }

    private class FakeProcessExecutor(
        private val result: ProcessResult
    ) : RoutesProcessExecutor {
        override suspend fun run(project: com.intellij.openapi.project.Project): ProcessResult = result
    }

    private class HangingExecutor : RoutesProcessExecutor {
        override suspend fun run(project: com.intellij.openapi.project.Project): ProcessResult {
            delay(10_000)
            return ProcessResult("", "", 0, false)
        }
    }

    fun testUpdatesRunningThenSuccessWithProgressTitle() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val ui = RecordingUi()
        val progress = RecordingProgressRunner()
        val executor = FakeProcessExecutor(
            ProcessResult(stdout = "ok", stderr = "", exitCode = 0, wasCancelled = false)
        )
        val service = RoutesRunnerService(provider, executor, ui, progress)

        service.requestRoutes(project)

        assertEquals("Running rails routes", progress.title)
        assertTrue(ui.states.first() is RouteLoadState.Running)
        assertTrue(ui.states.last() is RouteLoadState.Success)
    }

    fun testSuppressesConcurrentRequestsWhileActive() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val ui = RecordingUi()
        val progress = RecordingProgressRunner()
        val executor = HangingExecutor()
        val service = RoutesRunnerService(provider, executor, ui, progress)

        service.requestRoutes(project)
        service.requestRoutes(project) // ignored while first is active

        delay(50)

        assertTrue(ui.states.size == 1)
        assertTrue(ui.states.first() is RouteLoadState.Running)
    }

    fun testReportsErrorWhenExecutorThrows() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val ui = RecordingUi()
        val progress = RecordingProgressRunner()
        val executor = object : RoutesProcessExecutor {
            override suspend fun run(project: com.intellij.openapi.project.Project): ProcessResult {
                throw IllegalStateException("boom")
            }
        }
        val service = RoutesRunnerService(provider, executor, ui, progress)

        service.requestRoutes(project)

        assertTrue(ui.states.first() is RouteLoadState.Running)
        assertTrue(ui.states.last() is RouteLoadState.Error)
    }

    private class RecordingObservability : RoutesObservability {
        val transitions = mutableListOf<RouteLoadState>()
        val blockingWarnings = mutableListOf<String>()
        val threadViolations = mutableListOf<Pair<Boolean, String>>()

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

    fun testEmitsStateTransitionsAndCompletion() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val ui = RecordingUi()
        val progress = RecordingProgressRunner()
        val observability = RecordingObservability()
        val executor = FakeProcessExecutor(
            ProcessResult(stdout = "ok", stderr = "", exitCode = 0, wasCancelled = false)
        )
        val service = RoutesRunnerService(provider, executor, ui, progress, errorReporter = RecordingErrorReporter(), observability = observability)

        service.requestRoutes(project)

        assertEquals(listOf(RouteLoadState.Running, RouteLoadState.Success(emptyList(), null)), observability.transitions)
    }

    fun testWarnsBlockingWhenStartedOnEdt() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val ui = RecordingUi()
        val progress = RecordingProgressRunner()
        val observability = RecordingObservability()
        val executor = FakeProcessExecutor(
            ProcessResult(stdout = "ok", stderr = "", exitCode = 0, wasCancelled = false)
        )
        val service = RoutesRunnerService(provider, executor, ui, progress, errorReporter = RecordingErrorReporter(), observability = observability)

        com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
            service.requestRoutes(project)
        }
        kotlinx.coroutines.delay(10)

        assertTrue(observability.blockingWarnings.isNotEmpty())
    }

    private class RecordingErrorReporter : RoutesErrorReporter {
        val notifications = mutableListOf<Pair<String, Throwable?>>()
        override fun notify(project: com.intellij.openapi.project.Project, message: String, throwable: Throwable?) {
            notifications.add(message to throwable)
        }
    }

    fun testNotifiesOnNonZeroExit() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val ui = RecordingUi()
        val progress = RecordingProgressRunner()
        val reporter = RecordingErrorReporter()
        val executor = FakeProcessExecutor(
            ProcessResult(stdout = "", stderr = "boom", exitCode = 1, wasCancelled = false)
        )
        val service = RoutesRunnerService(provider, executor, ui, progress, reporter)

        service.requestRoutes(project)

        assertTrue(ui.states.last() is RouteLoadState.Error)
        assertEquals(1, reporter.notifications.size)
        val (message, throwable) = reporter.notifications.single()
        assertTrue(message.contains("exit 1"))
        assertTrue(message.contains("boom"))
        assertNull(throwable)
    }

    fun testReportsErrorAndUsesProgressRunnerOnFailure() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val ui = RecordingUi()
        val progress = RecordingProgressRunner()
        val reporter = RecordingErrorReporter()
        val executor = FakeProcessExecutor(
            ProcessResult(stdout = "", stderr = "boom", exitCode = 2, wasCancelled = false)
        )
        val service = RoutesRunnerService(provider, executor, ui, progress, reporter)

        service.requestRoutes(project)

        assertEquals("Running rails routes", progress.title)
        assertEquals(listOf(RouteLoadState.Running, RouteLoadState.Error("rails routes failed (exit 2): boom")), ui.states)
        assertEquals(1, reporter.notifications.size)
    }

    fun testExceptionIsReportedAndScopeStaysUsable() = runBlocking {
        val provider = ProjectCoroutineScopeProvider(testDispatchers)
        val ui = RecordingUi()
        val progress = RecordingProgressRunner()
        val reporter = RecordingErrorReporter()
        val failingExecutor = object : RoutesProcessExecutor {
            override suspend fun run(project: com.intellij.openapi.project.Project): ProcessResult {
                throw IllegalStateException("boom")
            }
        }
        val service = RoutesRunnerService(provider, failingExecutor, ui, progress, reporter)

        service.requestRoutes(project)

        assertTrue(ui.states.last() is RouteLoadState.Error)
        assertEquals(1, reporter.notifications.size)
        assertTrue(reporter.notifications.single().first.contains("boom"))
        assertTrue(reporter.notifications.single().second is IllegalStateException)

        val recoveryUi = RecordingUi()
        val successReporter = RecordingErrorReporter()
        val executor = FakeProcessExecutor(
            ProcessResult(stdout = "ok", stderr = "", exitCode = 0, wasCancelled = false)
        )
        val recoveryService = RoutesRunnerService(provider, executor, recoveryUi, progress, successReporter)

        recoveryService.requestRoutes(project)

        assertTrue(recoveryUi.states.last() is RouteLoadState.Success)
        assertTrue(successReporter.notifications.isEmpty())
    }
}
